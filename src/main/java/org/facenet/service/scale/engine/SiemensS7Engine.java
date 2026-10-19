package org.facenet.service.scale.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.DataField;
import org.facenet.event.MeasurementEvent;
import org.facenet.service.scale.engine.util.ConnectionFailureTracker;
import org.facenet.service.scale.engine.util.DataValueTransformer;
import org.facenet.service.scale.engine.util.ProtocolRuntimeDependencies;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** Polling engine for Siemens S7 PLCs over ISO-on-TCP using Apache PLC4X. */
@Slf4j
public class SiemensS7Engine implements ScaleEngine {

    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final long OFFLINE_THRESHOLD_MS = 60_000L;

    private final ScaleConfig config;
    private final BlockingQueue<MeasurementEvent> queue;
    private final ConnectionFailureTracker failureTracker = new ConnectionFailureTracker();

    private volatile boolean stopped;
    private volatile boolean running;
    private volatile PlcConnection connection;

    public SiemensS7Engine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        ProtocolRuntimeDependencies.prepareSiemensS7();
        this.config = config;
        this.queue = queue;
    }

    @Override
    public void run() {
        running = true;
        String connectionString = buildConnectionString(config.getConnParams());
        log.info("[Engine {}] Siemens S7 engine started ({})", config.getScaleId(), safeEndpoint());

        try {
            while (!stopped) {
                try {
                    ensureConnected(connectionString);
                    MeasurementEvent event = readMeasurement();
                    failureTracker.recordSuccess();
                    queue.put(event);
                    Thread.sleep(pollInterval());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception exception) {
                    log.error("[Engine {}] Siemens S7 read failed: {}", config.getScaleId(), exception.getMessage());
                    publishOfflineIfDue(exception);
                    closeConnection();
                    sleepReconnectDelay();
                }
            }
        } finally {
            closeConnection();
            running = false;
            log.info("[Engine {}] Siemens S7 engine stopped", config.getScaleId());
        }
    }

    private void ensureConnected(String connectionString) throws Exception {
        if (connection != null && connection.isConnected()) return;
        closeConnection();
        connection = PlcDriverManager.getDefault().getConnectionManager().getConnection(connectionString);
        if (!connection.isConnected()) connection.connect();
        if (!connection.getMetadata().isReadSupported()) {
            throw new IllegalStateException("S7 connection does not support reading");
        }
        log.info("[Engine {}] Connected to Siemens S7 endpoint {}", config.getScaleId(), safeEndpoint());
    }

    private MeasurementEvent readMeasurement() throws Exception {
        Map<String, Map<String, Object>> fields = configuredFields();
        if (fields.isEmpty()) throw new IllegalStateException("No enabled S7 data field is configured");

        PlcReadRequest.Builder builder = connection.readRequestBuilder();
        fields.forEach((alias, field) -> builder.addTagAddress(alias, field.get("tag_address").toString()));
        PlcReadResponse response = builder.build().execute().get(readTimeout(), TimeUnit.MILLISECONDS);

        Map<String, DataField> values = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : fields.entrySet()) {
            String alias = entry.getKey();
            PlcResponseCode responseCode = response.getResponseCode(alias);
            if (responseCode != PlcResponseCode.OK) {
                throw new IllegalStateException("S7 tag " + alias + " returned " + responseCode);
            }
            if (response.getNumberOfValues(alias) != 1) {
                throw new IllegalStateException("S7 tag " + alias + " must return exactly one value");
            }
            Map<String, Object> field = entry.getValue();
            values.put(alias, DataField.builder()
                    .name(text(field, "name"))
                    .value(DataValueTransformer.transform(response.getObject(alias), field))
                    .build());
        }

        return MeasurementEvent.builder()
                .scaleId(config.getScaleId())
                .lastTime(ZonedDateTime.now())
                .status("ONLINE")
                .data1(values.get("data1"))
                .data2(values.get("data2"))
                .data3(values.get("data3"))
                .data4(values.get("data4"))
                .data5(values.get("data5"))
                .build();
    }

    private Map<String, Map<String, Object>> configuredFields() {
        Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
        addField(fields, "data1", config.getData1());
        addField(fields, "data2", config.getData2());
        addField(fields, "data3", config.getData3());
        addField(fields, "data4", config.getData4());
        addField(fields, "data5", config.getData5());
        return fields;
    }

    private void addField(Map<String, Map<String, Object>> fields, String alias, Map<String, Object> field) {
        if (field != null && bool(field.get("is_used"), field.get("used"))) fields.put(alias, field);
    }

    static String buildConnectionString(Map<String, Object> params) {
        String explicit = text(params, "connection_string");
        if (explicit != null) return explicit;

        String ip = text(params, "ip");
        if (ip == null) throw new IllegalArgumentException("Siemens S7 connection requires ip");
        int port = integer(params, "port", 102);
        if (port < 1 || port > 65_535) throw new IllegalArgumentException("Siemens S7 port must be between 1 and 65535");
        int rack = integer(params, "rack", 0);
        int slot = integer(params, "slot", 1);
        int timeout = integer(params, "read_timeout_ms", DEFAULT_TIMEOUT_MS);
        String controllerType = text(params, "controller_type");

        StringBuilder result = new StringBuilder("s7://").append(ip).append(':').append(port)
                .append("?remote-rack=").append(rack)
                .append("&remote-slot=").append(slot)
                .append("&read-timeout=").append(timeout)
                .append("&tcp.default-timeout=").append(integer(params, "connect_timeout_ms", DEFAULT_TIMEOUT_MS));
        if (controllerType != null) {
            result.append("&controller-type=").append(controllerType.toUpperCase(Locale.ROOT).replace('-', '_'));
        }
        if (params != null && params.get("local_tsap") != null) {
            result.append("&local-tsap=").append(integer(params, "local_tsap", 0));
        }
        if (params != null && params.get("remote_tsap") != null) {
            result.append("&remote-tsap=").append(integer(params, "remote_tsap", 0));
        }
        return result.toString();
    }

    private int readTimeout() {
        return integer(config.getConnParams(), "read_timeout_ms", DEFAULT_TIMEOUT_MS);
    }

    private int pollInterval() {
        return config.getPollInterval() != null && config.getPollInterval() > 0 ? config.getPollInterval() : 1000;
    }

    private String safeEndpoint() {
        return text(config.getConnParams(), "ip") + ":" + integer(config.getConnParams(), "port", 102) + ", rack="
                + integer(config.getConnParams(), "rack", 0) + ", slot="
                + integer(config.getConnParams(), "slot", 1);
    }

    private void publishOfflineIfDue(Exception cause) {
        if (!failureTracker.shouldPublishOffline(System.currentTimeMillis(), OFFLINE_THRESHOLD_MS)) return;
        MeasurementEvent event = MeasurementEvent.builder()
                .scaleId(config.getScaleId()).lastTime(ZonedDateTime.now()).status("OFFLINE").build();
        if (queue.offer(event)) {
            failureTracker.markOfflinePublished();
            log.warn("[Engine {}] Siemens S7 marked OFFLINE: {}", config.getScaleId(), cause.getMessage());
        }
    }

    private void sleepReconnectDelay() {
        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            stopped = true;
        }
    }

    private void closeConnection() {
        PlcConnection current = connection;
        connection = null;
        if (current == null) return;
        try {
            current.close();
        } catch (Exception exception) {
            log.debug("[Engine {}] Error closing S7 connection: {}", config.getScaleId(), exception.getMessage());
        }
    }

    private static String text(Map<String, Object> values, String key) {
        if (values == null || values.get(key) == null || values.get(key).toString().isBlank()) return null;
        return values.get(key).toString().trim();
    }

    private static int integer(Map<String, Object> values, String key, int defaultValue) {
        String value = text(values, key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private static boolean bool(Object primary, Object alias) {
        Object value = primary != null ? primary : alias;
        return value instanceof Boolean booleanValue ? booleanValue
                : value != null && (value.toString().equalsIgnoreCase("true") || value.toString().equals("1"));
    }

    @Override
    public void stop() {
        stopped = true;
        closeConnection();
    }

    @Override
    public Long getScaleId() {
        return config.getScaleId();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
