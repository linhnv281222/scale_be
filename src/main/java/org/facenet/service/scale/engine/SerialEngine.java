package org.facenet.service.scale.engine;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.DataField;
import org.facenet.event.MeasurementEvent;
import org.facenet.service.scale.engine.util.ConnectionFailureTracker;
import org.facenet.service.scale.engine.util.DataValueTransformer;
import org.facenet.service.scale.engine.util.ProtocolRuntimeDependencies;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generic serial ASCII engine for streaming and request/response devices. */
@Slf4j
public class SerialEngine implements ScaleEngine {

    private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 3000;
    private static final int DEFAULT_RECONNECT_DELAY_MS = 5000;
    private static final long DEFAULT_OFFLINE_THRESHOLD_MS = 60_000L;
    private static final int DEFAULT_MAX_RESPONSE_BYTES = 1024;
    private static final Pattern NUMBER = Pattern.compile(
            "[+-]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)(?:[Ee][+-]?\\d+)?");

    private final ScaleConfig config;
    private final BlockingQueue<MeasurementEvent> queue;
    private final ConnectionFailureTracker failureTracker = new ConnectionFailureTracker();
    private volatile boolean stopped;
    private volatile boolean running;
    private volatile SerialPort serialPort;

    public SerialEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        ProtocolRuntimeDependencies.prepareDirectSerial("Serial");
        this.config = config;
        this.queue = queue;
    }

    @Override
    public void run() {
        running = true;
        log.info("[Engine {}] Generic serial engine started", config.getScaleId());
        try {
            while (!stopped) {
                try {
                    ensureConnected();
                    queue.put(readMeasurement());
                    failureTracker.recordSuccess();
                    Thread.sleep(pollInterval());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception exception) {
                    log.error("[Engine {}] Serial read failed: {}", config.getScaleId(), exception.getMessage());
                    publishOfflineIfDue(exception);
                    closePort();
                    sleepReconnectDelay();
                }
            }
        } finally {
            closePort();
            running = false;
            log.info("[Engine {}] Generic serial engine stopped", config.getScaleId());
        }
    }

    private void ensureConnected() {
        SerialPort current = serialPort;
        if (current != null && current.isOpen()) return;
        Map<String, Object> params = config.getConnParams();
        String portName = text(params, "com_port");
        if (portName == null) throw new IllegalArgumentException("Serial connection requires com_port");

        SerialPort port = SerialPort.getCommPort(portName);
        port.setComPortParameters(integer(params, "baud_rate", 9600),
                integer(params, "data_bits", 8), stopBits(params), parity(params));
        int timeout = integer(params, "response_timeout_ms", DEFAULT_RESPONSE_TIMEOUT_MS);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        if (!port.openPort()) throw new IllegalStateException("Cannot open serial port " + portName);
        serialPort = port;
        log.info("[Engine {}] Connected to serial port {}", config.getScaleId(), portName);
    }

    private MeasurementEvent readMeasurement() throws Exception {
        List<Map<String, Object>> fields = List.of(
                nullSafe(config.getData1()), nullSafe(config.getData2()), nullSafe(config.getData3()),
                nullSafe(config.getData4()), nullSafe(config.getData5()));
        if (fields.stream().noneMatch(SerialEngine::isUsed)) {
            throw new IllegalStateException("No enabled serial data field is configured");
        }

        String sharedResponse = null;
        DataField[] values = new DataField[5];
        for (int index = 0; index < fields.size(); index++) {
            Map<String, Object> field = fields.get(index);
            if (!isUsed(field)) continue;
            String command = text(field, "command");
            String response;
            if (command == null) {
                if (sharedResponse == null) sharedResponse = exchange(null);
                response = sharedResponse;
            } else {
                response = exchange(buildCommand(config.getConnParams(), field));
            }
            values[index] = DataField.builder().name(text(field, "name"))
                    .value(parseResponse(response, field)).build();
        }

        return MeasurementEvent.builder().scaleId(config.getScaleId()).lastTime(ZonedDateTime.now())
                .status("ONLINE").data1(values[0]).data2(values[1]).data3(values[2])
                .data4(values[3]).data5(values[4]).build();
    }

    private String exchange(String request) throws IOException {
        SerialPort port = serialPort;
        if (port == null || !port.isOpen()) throw new IOException("Serial port is not open");
        if (request != null) {
            port.flushIOBuffers();
            OutputStream output = port.getOutputStream();
            output.write(request.getBytes(charset(config.getConnParams())));
            output.flush();
        }
        String response = readFrame(port.getInputStream(), config.getConnParams());
        if (request != null && booleanValue(config.getConnParams(), "strip_echo", true)
                && stripLineEndings(response).equals(stripLineEndings(request))) {
            response = readFrame(port.getInputStream(), config.getConnParams());
        }
        if (response.isBlank()) throw new IOException("Empty serial response");
        return response;
    }

    static String buildCommand(Map<String, Object> connection, Map<String, Object> field) {
        String command = text(field, "command");
        if (command == null) throw new IllegalArgumentException("Serial command must not be blank");
        String prefix = valueOrDefault(text(connection, "command_prefix"), "");
        String terminator = decodeEscapes(valueOrDefault(text(connection, "command_terminator"), "\\r"));
        return prefix + decodeEscapes(command) + terminator;
    }

    static String parseResponse(String rawResponse, Map<String, Object> field) {
        if (rawResponse == null || rawResponse.isBlank()) throw new IllegalArgumentException("Empty serial response");
        String response = rawResponse.replace("\0", "").trim();
        String candidate = response;

        String configuredPattern = text(field, "response_pattern");
        if (configuredPattern != null) {
            Matcher matcher = Pattern.compile(configuredPattern).matcher(response);
            if (!matcher.find() || matcher.groupCount() < 1) {
                throw new IllegalArgumentException("Serial response does not match response_pattern");
            }
            candidate = matcher.group(1);
        } else if (text(field, "delimiter") != null) {
            String[] parts = response.split(Pattern.quote(decodeEscapes(text(field, "delimiter"))), -1);
            int valueIndex = integer(field, "value_index", 0);
            if (valueIndex < 0 || valueIndex >= parts.length) {
                throw new IllegalArgumentException("Serial value_index is outside the response columns");
            }
            candidate = parts[valueIndex];
        } else if (field.get("start_index") != null) {
            int start = integer(field, "start_index", -1);
            int length = integer(field, "length", response.length() - start);
            if (start < 0 || length <= 0 || start + length > response.length()) {
                throw new IllegalArgumentException("Serial fixed-width range is outside the response");
            }
            candidate = response.substring(start, start + length);
        }

        candidate = candidate.trim();
        String valueType = valueOrDefault(text(field, "value_type"),
                valueOrDefault(text(field, "data_type"), "NUMBER")).toUpperCase(Locale.ROOT);
        if (valueType.equals("TEXT") || valueType.equals("STRING")) {
            return DataValueTransformer.transform(candidate, field);
        }
        Matcher number = NUMBER.matcher(candidate);
        if (!number.find()) throw new IllegalArgumentException("No numeric value in serial response: " + response);
        String numericText = number.group();
        if (numericText.indexOf(',') >= 0 && numericText.indexOf('.') < 0) numericText = numericText.replace(',', '.');
        return DataValueTransformer.transform(new BigDecimal(numericText), field);
    }

    static String readFrame(InputStream input, Map<String, Object> connection) throws IOException {
        Charset charset = charset(connection);
        byte[] terminator = decodeEscapes(valueOrDefault(text(connection, "response_terminator"), "\\r"))
                .getBytes(charset);
        int maximum = integer(connection, "max_response_bytes", DEFAULT_MAX_RESPONSE_BYTES);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (bytes.size() < maximum) {
            int value = input.read();
            if (value < 0) break;
            bytes.write(value);
            if (terminator.length > 0 && endsWith(bytes.toByteArray(), terminator)) break;
        }
        return bytes.toString(charset);
    }

    private void publishOfflineIfDue(Exception cause) {
        long threshold = longInteger(config.getConnParams(), "offline_threshold_ms", DEFAULT_OFFLINE_THRESHOLD_MS);
        if (!failureTracker.shouldPublishOffline(System.currentTimeMillis(), threshold)) return;
        MeasurementEvent event = MeasurementEvent.builder().scaleId(config.getScaleId())
                .lastTime(ZonedDateTime.now()).status("OFFLINE").build();
        if (queue.offer(event)) {
            failureTracker.markOfflinePublished();
            log.warn("[Engine {}] Serial device marked OFFLINE: {}", config.getScaleId(), cause.getMessage());
        }
    }

    private void sleepReconnectDelay() {
        int delay = integer(config.getConnParams(), "reconnect_delay_ms", DEFAULT_RECONNECT_DELAY_MS);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            stopped = true;
        }
    }

    private int pollInterval() {
        return config.getPollInterval() != null && config.getPollInterval() > 0 ? config.getPollInterval() : 1000;
    }

    private static boolean endsWith(byte[] value, byte[] suffix) {
        if (suffix.length > value.length) return false;
        int offset = value.length - suffix.length;
        for (int index = 0; index < suffix.length; index++) if (value[offset + index] != suffix[index]) return false;
        return true;
    }

    private static Map<String, Object> nullSafe(Map<String, Object> value) { return value == null ? Map.of() : value; }

    private static boolean isUsed(Map<String, Object> field) {
        return booleanValue(field, "is_used", false) || booleanValue(field, "used", false);
    }

    private static boolean booleanValue(Map<String, Object> values, String key, boolean fallback) {
        if (values == null || values.get(key) == null) return fallback;
        Object value = values.get(key);
        if (value instanceof Boolean bool) return bool;
        return value.toString().equalsIgnoreCase("true") || value.toString().equals("1");
    }

    private static Charset charset(Map<String, Object> connection) {
        String name = text(connection, "charset");
        return name == null ? StandardCharsets.US_ASCII : Charset.forName(name);
    }

    private static int stopBits(Map<String, Object> params) {
        return switch (valueOrDefault(text(params, "stop_bits"), "1")) {
            case "1.5" -> SerialPort.ONE_POINT_FIVE_STOP_BITS;
            case "2", "2.0" -> SerialPort.TWO_STOP_BITS;
            default -> SerialPort.ONE_STOP_BIT;
        };
    }

    private static int parity(Map<String, Object> params) {
        return switch (valueOrDefault(text(params, "parity"), "NONE").toUpperCase(Locale.ROOT)) {
            case "EVEN" -> SerialPort.EVEN_PARITY;
            case "ODD" -> SerialPort.ODD_PARITY;
            case "MARK" -> SerialPort.MARK_PARITY;
            case "SPACE" -> SerialPort.SPACE_PARITY;
            default -> SerialPort.NO_PARITY;
        };
    }

    private static String decodeEscapes(String value) {
        return value.replace("\\r", "\r").replace("\\n", "\n").replace("\\t", "\t");
    }

    private static String stripLineEndings(String value) { return value == null ? "" : value.replaceAll("[\\r\\n]+$", ""); }

    private static String text(Map<String, Object> values, String key) {
        if (values == null || values.get(key) == null || values.get(key).toString().isBlank()) return null;
        return values.get(key).toString().trim();
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        String value = text(values, key);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static long longInteger(Map<String, Object> values, String key, long fallback) {
        String value = text(values, key);
        return value == null ? fallback : Long.parseLong(value);
    }

    private static String valueOrDefault(String value, String fallback) { return value == null ? fallback : value; }

    private void closePort() {
        SerialPort current = serialPort;
        serialPort = null;
        if (current != null && current.isOpen()) current.closePort();
    }

    @Override
    public void stop() { stopped = true; closePort(); }

    @Override
    public Long getScaleId() { return config.getScaleId(); }

    @Override
    public boolean isRunning() { return running && !stopped; }
}
