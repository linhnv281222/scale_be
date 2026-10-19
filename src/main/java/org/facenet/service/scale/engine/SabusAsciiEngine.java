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
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SABUS ASCII worker. A physical COM port is opened once and all configured
 * stations on that bus are polled sequentially with commands such as
 * {@code !01FL\r} (instantaneous flow) and {@code !01NT\r} (accumulated total).
 */
@Slf4j
public class SabusAsciiEngine implements ScaleEngine {

    private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 3000;
    private static final int DEFAULT_RECONNECT_DELAY_MS = 5000;
    private static final long DEFAULT_OFFLINE_THRESHOLD_MS = 60_000L;
    private static final int IDLE_SLEEP_MS = 25;
    private static final Pattern NUMBER = Pattern.compile(
            "[+-]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)(?:[Ee][+-]?\\d+)?");

    private final List<ScaleConfig> configs;
    private final BlockingQueue<MeasurementEvent> queue;
    private final Map<Long, ConnectionFailureTracker> failureStates = new ConcurrentHashMap<>();
    private final Map<Long, Long> nextPollAt = new ConcurrentHashMap<>();
    private final String busKey;

    private volatile boolean stopped;
    private volatile boolean running;
    private volatile SerialPort serialPort;

    public SabusAsciiEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        this(List.of(config), queue);
    }

    public SabusAsciiEngine(List<ScaleConfig> configs, BlockingQueue<MeasurementEvent> queue) {
        ProtocolRuntimeDependencies.prepareDirectSerial("SABUS");
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("At least one SABUS configuration is required");
        }
        this.configs = List.copyOf(configs);
        this.queue = queue;
        this.busKey = busKey(configs.get(0));
        for (ScaleConfig config : configs) {
            if (!busKey.equals(busKey(config))) {
                throw new IllegalArgumentException("All SABUS configurations must use the same serial bus");
            }
            failureStates.put(config.getScaleId(), new ConnectionFailureTracker());
            nextPollAt.put(config.getScaleId(), 0L);
        }
    }

    @Override
    public void run() {
        running = true;
        log.info("[SABUS Bus {}] Starting sequential polling for scales {}", busKey, getScaleIds());
        try {
            while (!stopped) {
                try {
                    ensureConnected();
                    pollDueScales();
                    Thread.sleep(IDLE_SLEEP_MS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception exception) {
                    log.error("[SABUS Bus {}] Bus error: {}", busKey, exception.getMessage());
                    publishBusFailures(exception);
                    closePort();
                    sleepReconnectDelay();
                }
            }
        } finally {
            closePort();
            running = false;
            log.info("[SABUS Bus {}] Stopped", busKey);
        }
    }

    private void ensureConnected() {
        SerialPort current = serialPort;
        if (current != null && current.isOpen()) return;

        ScaleConfig busConfig = configs.get(0);
        Map<String, Object> params = busConfig.getConnParams();
        String portName = text(params, "com_port");
        int baudRate = integer(params, "baud_rate", 9600);
        if (portName == null) throw new IllegalArgumentException("SABUS connection requires com_port");

        SerialPort port = SerialPort.getCommPort(portName);
        port.setComPortParameters(baudRate, integer(params, "data_bits", 8),
                stopBits(params), parity(params));
        int timeout = integer(params, "response_timeout_ms", DEFAULT_RESPONSE_TIMEOUT_MS);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);
        if (!port.openPort()) throw new IllegalStateException("Cannot open SABUS port " + portName);
        serialPort = port;
        log.info("[SABUS Bus {}] Connected", busKey);
    }

    private void pollDueScales() throws Exception {
        long now = System.currentTimeMillis();
        for (ScaleConfig config : configs) {
            if (stopped) return;
            if (now < nextPollAt.getOrDefault(config.getScaleId(), 0L)) continue;
            nextPollAt.put(config.getScaleId(), now + pollInterval(config));
            try {
                MeasurementEvent event = readMeasurement(config);
                failureStates.get(config.getScaleId()).recordSuccess();
                queue.put(event);
            } catch (InterruptedException exception) {
                throw exception;
            } catch (Exception exception) {
                log.warn("[SABUS Bus {}] Scale {} read failed: {}", busKey,
                        config.getScaleId(), exception.getMessage());
                publishOfflineIfDue(config, exception);
            }
        }
    }

    private MeasurementEvent readMeasurement(ScaleConfig config) throws Exception {
        if (!hasConfiguredField(config)) {
            throw new IllegalStateException("No enabled SABUS data field is configured");
        }
        return MeasurementEvent.builder()
                .scaleId(config.getScaleId())
                .lastTime(ZonedDateTime.now())
                .status("ONLINE")
                .data1(readDataField(config, config.getData1()))
                .data2(readDataField(config, config.getData2()))
                .data3(readDataField(config, config.getData3()))
                .data4(readDataField(config, config.getData4()))
                .data5(readDataField(config, config.getData5()))
                .build();
    }

    private DataField readDataField(ScaleConfig config, Map<String, Object> field) throws Exception {
        if (!isUsed(field)) return null;
        String command = text(field, "command");
        String request = buildCommand(config.getConnParams(), field);
        String response = exchange(request);
        String value = parseResponse(response, deviceId(config.getConnParams()), command, field);
        return DataField.builder().name(text(field, "name")).value(value).build();
    }

    private String exchange(String request) throws IOException {
        SerialPort port = serialPort;
        if (port == null || !port.isOpen()) throw new IOException("SABUS serial port is not open");
        port.flushIOBuffers();
        OutputStream output = port.getOutputStream();
        output.write(request.getBytes(StandardCharsets.US_ASCII));
        output.flush();

        String response = readLine(port.getInputStream());
        if (response.equals(request.stripTrailing())) {
            response = readLine(port.getInputStream());
        }
        if (response.isBlank()) throw new IOException("Empty SABUS response");
        return response;
    }

    private String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (bytes.size() < 512) {
            int value = input.read();
            if (value < 0) break;
            if (value == '\r' || value == '\n') {
                if (bytes.size() > 0) break;
                continue;
            }
            bytes.write(value);
        }
        return bytes.toString(StandardCharsets.US_ASCII).trim();
    }

    static String buildCommand(Map<String, Object> connection, Map<String, Object> field) {
        String command = text(field, "command");
        if (command == null || !(command.equalsIgnoreCase("FL") || command.equalsIgnoreCase("NT"))) {
            throw new IllegalArgumentException("SABUS command must be FL or NT");
        }
        String prefix = valueOrDefault(text(connection, "command_prefix"), "!");
        String terminator = decodeTerminator(valueOrDefault(text(connection, "command_terminator"), "\\r"));
        return prefix + formattedDeviceId(connection) + command.toUpperCase(Locale.ROOT) + terminator;
    }

    static String parseResponse(String rawResponse, String deviceId, String command,
                                Map<String, Object> field) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("Empty SABUS response");
        }
        String response = rawResponse.replace("\0", "").trim();
        String upper = response.toUpperCase(Locale.ROOT);
        if (upper.contains("NAK") || upper.contains("ERROR") || upper.contains("ERR")) {
            throw new IllegalArgumentException("SABUS device returned an error: " + response);
        }

        String candidate = response;
        String configuredPattern = text(field, "response_pattern");
        if (configuredPattern != null) {
            Matcher configured = Pattern.compile(configuredPattern).matcher(response);
            if (!configured.find() || configured.groupCount() < 1) {
                throw new IllegalArgumentException("SABUS response does not match response_pattern");
            }
            candidate = configured.group(1);
        } else {
            int commandAt = upper.indexOf(command.toUpperCase(Locale.ROOT));
            if (commandAt >= 0) {
                candidate = response.substring(commandAt + command.length());
            } else {
                candidate = stripAddressPrefix(response, deviceId);
            }
        }

        Matcher number = NUMBER.matcher(candidate);
        if (!number.find()) {
            throw new IllegalArgumentException("No numeric value in SABUS response: " + response);
        }
        String numericText = number.group();
        if (numericText.indexOf(',') >= 0 && numericText.indexOf('.') < 0) {
            numericText = numericText.replace(',', '.');
        }
        BigDecimal value = new BigDecimal(numericText);
        return DataValueTransformer.transform(value, field);
    }

    private static String stripAddressPrefix(String response, String deviceId) {
        String value = response.startsWith("!") ? response.substring(1) : response;
        if (deviceId != null && value.startsWith(deviceId)) value = value.substring(deviceId.length());
        return value.replaceFirst("^[\\s=:;,]+", "");
    }

    private void publishBusFailures(Exception cause) {
        configs.forEach(config -> publishOfflineIfDue(config, cause));
    }

    private void publishOfflineIfDue(ScaleConfig config, Exception cause) {
        long threshold = longInteger(config.getConnParams(), "offline_threshold_ms", DEFAULT_OFFLINE_THRESHOLD_MS);
        ConnectionFailureTracker tracker = failureStates.get(config.getScaleId());
        if (!tracker.shouldPublishOffline(System.currentTimeMillis(), threshold)) return;
        MeasurementEvent event = MeasurementEvent.builder().scaleId(config.getScaleId())
                .lastTime(ZonedDateTime.now()).status("OFFLINE").build();
        if (queue.offer(event)) {
            tracker.markOfflinePublished();
            log.warn("[SABUS Bus {}] Scale {} marked OFFLINE: {}", busKey,
                    config.getScaleId(), cause.getMessage());
        }
    }

    private void sleepReconnectDelay() {
        int delay = integer(configs.get(0).getConnParams(), "reconnect_delay_ms", DEFAULT_RECONNECT_DELAY_MS);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            stopped = true;
        }
    }

    private boolean hasConfiguredField(ScaleConfig config) {
        return isUsed(config.getData1()) || isUsed(config.getData2()) || isUsed(config.getData3())
                || isUsed(config.getData4()) || isUsed(config.getData5());
    }

    private static boolean isUsed(Map<String, Object> field) {
        if (field == null) return false;
        Object value = field.get("is_used") != null ? field.get("is_used") : field.get("used");
        return value instanceof Boolean bool ? bool
                : value != null && (value.toString().equalsIgnoreCase("true") || value.toString().equals("1"));
    }

    private int pollInterval(ScaleConfig config) {
        return config.getPollInterval() != null && config.getPollInterval() > 0
                ? config.getPollInterval() : 1000;
    }

    public static String busKey(ScaleConfig config) {
        Map<String, Object> params = config.getConnParams();
        return String.join("|", String.valueOf(text(params, "com_port")),
                String.valueOf(integer(params, "baud_rate", 9600)),
                String.valueOf(integer(params, "data_bits", 8)),
                String.valueOf(integer(params, "stop_bits", 1)),
                valueOrDefault(text(params, "parity"), "NONE").toUpperCase(Locale.ROOT));
    }

    public Set<Long> getScaleIds() {
        Set<Long> result = new LinkedHashSet<>();
        configs.forEach(config -> result.add(config.getScaleId()));
        return result;
    }

    private static String formattedDeviceId(Map<String, Object> connection) {
        String id = deviceId(connection);
        if (id == null || id.isBlank()) throw new IllegalArgumentException("SABUS connection requires device_id");
        int width = integer(connection, "device_id_width", 0);
        if (width > 0 && id.length() < width) id = "0".repeat(width - id.length()) + id;
        return id;
    }

    private static String deviceId(Map<String, Object> connection) {
        String id = text(connection, "device_id");
        if (id == null) id = text(connection, "station_id");
        if (id == null) id = text(connection, "id");
        return id;
    }

    private static String decodeTerminator(String value) {
        return value.replace("\\r", "\r").replace("\\n", "\n");
    }

    private static int stopBits(Map<String, Object> params) {
        return switch (integer(params, "stop_bits", 1)) {
            case 2 -> SerialPort.TWO_STOP_BITS;
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

    private static String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private void closePort() {
        SerialPort current = serialPort;
        serialPort = null;
        if (current != null && current.isOpen()) current.closePort();
    }

    @Override
    public void stop() {
        stopped = true;
        closePort();
    }

    @Override
    public Long getScaleId() {
        return configs.get(0).getScaleId();
    }

    @Override
    public boolean isRunning() {
        return running && !stopped;
    }
}
