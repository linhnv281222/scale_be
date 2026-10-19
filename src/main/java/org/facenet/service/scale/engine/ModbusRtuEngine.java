package org.facenet.service.scale.engine;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.serial.SerialParameters;
import com.intelligt.modbus.jlibmodbus.serial.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.DataField;
import org.facenet.event.MeasurementEvent;
import org.facenet.service.scale.engine.util.ModbusDataConverter;
import org.facenet.service.scale.engine.util.ConnectionFailureTracker;
import org.facenet.service.scale.engine.util.ProtocolRuntimeDependencies;

import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One Modbus RTU worker per physical serial bus. All scales with the same serial
 * parameters are polled sequentially, so a COM port is opened exactly once.
 */
@Slf4j
public class ModbusRtuEngine implements ScaleEngine {

    private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 3000;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final long OFFLINE_THRESHOLD_MS = 60_000L;
    private static final int IDLE_SLEEP_MS = 25;

    private final List<ScaleConfig> configs;
    private final BlockingQueue<MeasurementEvent> queue;
    private final Map<Long, ConnectionFailureTracker> failureStates = new ConcurrentHashMap<>();
    private final Map<Long, Long> nextPollAt = new ConcurrentHashMap<>();
    private final String busKey;

    private volatile boolean stopped = false;
    private volatile boolean running = false;
    private volatile ModbusMaster master;

    public ModbusRtuEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        this(List.of(config), queue);
    }

    public ModbusRtuEngine(List<ScaleConfig> configs, BlockingQueue<MeasurementEvent> queue) {
        ProtocolRuntimeDependencies.prepareModbusRtu();
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("At least one RTU scale configuration is required");
        }
        this.configs = List.copyOf(configs);
        this.queue = queue;
        this.busKey = busKey(configs.get(0));

        for (ScaleConfig config : configs) {
            if (!busKey.equals(busKey(config))) {
                throw new IllegalArgumentException("All RTU configurations must use the same serial bus");
            }
            failureStates.put(config.getScaleId(), new ConnectionFailureTracker());
            nextPollAt.put(config.getScaleId(), 0L);
        }
    }

    @Override
    public void run() {
        running = true;
        ScaleConfig busConfig = configs.get(0);
        String comPort = getString(busConfig.getConnParams(), "com_port");
        Integer baudRate = getInteger(busConfig.getConnParams(), "baud_rate");

        log.info("[RTU Bus {}] Starting sequential polling for scales {}", busKey, getScaleIds());

        try {
            if (comPort == null || baudRate == null) {
                throw new IllegalArgumentException("Missing com_port or baud_rate for RTU bus " + busKey);
            }

            SerialParameters parameters = new SerialParameters();
            parameters.setDevice(comPort);
            parameters.setBaudRate(SerialPort.BaudRate.getBaudRate(baudRate));
            parameters.setDataBits(orDefault(getInteger(busConfig.getConnParams(), "data_bits"), 8));
            parameters.setStopBits(orDefault(getInteger(busConfig.getConnParams(), "stop_bits"), 1));
            parameters.setParity(parseParity(getString(busConfig.getConnParams(), "parity")));

            master = ModbusMasterFactory.createModbusMasterRTU(parameters);
            Modbus.setAutoIncrementTransactionId(true);
            master.setResponseTimeout(orDefault(
                    getInteger(busConfig.getConnParams(), "response_timeout_ms"),
                    DEFAULT_RESPONSE_TIMEOUT_MS));

            log.info("[RTU Bus {}] Entering polling loop (stopped={}, threadInterrupted={})",
                    busKey, stopped, Thread.currentThread().isInterrupted());
            while (!stopped) {
                try {
                    ensureConnected();
                    pollDueScales();
                    Thread.sleep(IDLE_SLEEP_MS);
                } catch (InterruptedException e) {
                    log.error("[RTU Bus {}] Worker interrupted while connecting or polling", busKey, e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("[RTU Bus {}] Bus error: {}", busKey, e.getMessage());
                    publishBusFailures(e);
                    disconnect();
                    sleepReconnectDelay();
                }
            }
        } catch (Exception e) {
            log.error("[RTU Bus {}] Fatal error: {}", busKey, e.getMessage(), e);
            publishBusFailures(e);
        } catch (LinkageError e) {
            log.error("[RTU Bus {}] Fatal serial runtime/linkage error: {}", busKey, e.getMessage(), e);
        } catch (Error e) {
            log.error("[RTU Bus {}] Unexpected fatal JVM error: {}", busKey, e.getMessage(), e);
        } finally {
            disconnect();
            running = false;
            log.info("[RTU Bus {}] Stopped", busKey);
        }
    }

    private void ensureConnected() throws Exception {
        if (!master.isConnected()) {
            log.info("[RTU Bus {}] Opening serial port", busKey);
            master.connect();
            log.info("[RTU Bus {}] Connected", busKey);
        }
    }

    private void pollDueScales() throws InterruptedException {
        long now = System.currentTimeMillis();
        for (ScaleConfig config : configs) {
            if (stopped) {
                return;
            }
            if (now < nextPollAt.getOrDefault(config.getScaleId(), 0L)) {
                continue;
            }

            nextPollAt.put(config.getScaleId(), now + getPollIntervalMs(config));
            try {
                MeasurementEvent event = readMeasurement(config);
                failureStates.get(config.getScaleId()).recordSuccess();
                queue.put(event);
                log.debug("[RTU Bus {}] Read scale {} (unit {})", busKey,
                        config.getScaleId(), getUnitId(config));
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[RTU Bus {}] Scale {} read failed: {}", busKey,
                        config.getScaleId(), e.getMessage());
                publishOfflineIfDue(config, e);
                // A failed unit must not prevent polling the next unit on the bus.
            }
        }
    }

    private MeasurementEvent readMeasurement(ScaleConfig config) throws Exception {
        if (!hasConfiguredDataField(config)) {
            throw new IllegalStateException("No enabled data field is configured");
        }

        int unitId = getUnitId(config);
        DataField data1 = createDataField(config, unitId, config.getData1());
        DataField data2 = createDataField(config, unitId, config.getData2());
        DataField data3 = createDataField(config, unitId, config.getData3());
        DataField data4 = createDataField(config, unitId, config.getData4());
        DataField data5 = createDataField(config, unitId, config.getData5());

        return MeasurementEvent.builder()
                .scaleId(config.getScaleId())
                .lastTime(ZonedDateTime.now())
                .status("ONLINE")
                .data1(data1)
                .data2(data2)
                .data3(data3)
                .data4(data4)
                .data5(data5)
                .build();
    }

    private DataField createDataField(ScaleConfig config, int unitId,
                                      Map<String, Object> dataConfig) throws Exception {
        if (!isDataSlotUsed(dataConfig)) {
            return null;
        }

        Integer startAddress = getInteger(dataConfig,
                "start_registers", "start_register", "register_start");
        Integer numRegisters = getInteger(dataConfig, "num_registers", "register_count");
        if (startAddress == null || numRegisters == null || numRegisters <= 0) {
            throw new IllegalArgumentException("Invalid register config for scale " + config.getScaleId());
        }

        int[] registers = readRegisters(unitId, startAddress, numRegisters, dataConfig);
        String value = ModbusDataConverter.registersToString(registers, dataConfig);
        if (value == null) {
            throw new IllegalStateException("Unable to convert register value for scale " + config.getScaleId());
        }

        return DataField.builder()
                .name(getString(dataConfig, "name"))
                .value(value)
                .build();
    }

    private int[] readRegisters(int unitId, int startAddress, int numRegisters,
                                Map<String, Object> dataConfig) throws Exception {
        Integer functionCode = getInteger(dataConfig, "function_code", "function");
        int code = functionCode != null ? functionCode : 3;
        return switch (code) {
            case 3 -> master.readHoldingRegisters(unitId, startAddress, numRegisters);
            case 4 -> master.readInputRegisters(unitId, startAddress, numRegisters);
            default -> throw new IllegalArgumentException("Unsupported Modbus function_code " + code
                    + "; register fields support function 3 or 4");
        };
    }

    private void publishBusFailures(Exception cause) {
        for (ScaleConfig config : configs) {
            publishOfflineIfDue(config, cause);
        }
    }

    private void publishOfflineIfDue(ScaleConfig config, Exception cause) {
        long now = System.currentTimeMillis();
        ConnectionFailureTracker state = failureStates.get(config.getScaleId());
        if (!state.shouldPublishOffline(now, OFFLINE_THRESHOLD_MS)) {
            return;
        }

        MeasurementEvent offlineEvent = MeasurementEvent.builder()
                .scaleId(config.getScaleId())
                .lastTime(ZonedDateTime.now())
                .status("OFFLINE")
                .build();
        if (queue.offer(offlineEvent)) {
            state.markOfflinePublished();
            log.warn("[RTU Bus {}] Scale {} marked OFFLINE: {}", busKey,
                    config.getScaleId(), cause.getMessage());
        } else {
            log.error("[RTU Bus {}] Cannot publish OFFLINE for scale {} because queue is full",
                    busKey, config.getScaleId());
        }
    }

    private void sleepReconnectDelay() {
        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopped = true;
        }
    }

    private boolean hasConfiguredDataField(ScaleConfig config) {
        return isDataSlotUsed(config.getData1()) || isDataSlotUsed(config.getData2())
                || isDataSlotUsed(config.getData3()) || isDataSlotUsed(config.getData4())
                || isDataSlotUsed(config.getData5());
    }

    private boolean isDataSlotUsed(Map<String, Object> dataConfig) {
        if (dataConfig == null) {
            return false;
        }
        Object used = dataConfig.get("is_used");
        if (used == null) {
            used = dataConfig.get("used");
        }
        return toBoolean(used);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null) {
            return false;
        }
        String text = value.toString().trim().toLowerCase(Locale.ROOT);
        return text.equals("true") || text.equals("1") || text.equals("yes") || text.equals("y");
    }

    private int getUnitId(ScaleConfig config) {
        Integer unitId = getInteger(config.getConnParams(), "unit_id", "device_id", "id");
        return unitId != null ? unitId : 1;
    }

    private int getPollIntervalMs(ScaleConfig config) {
        Integer interval = config.getPollInterval();
        return interval != null && interval > 0 ? interval : 1000;
    }

    private static String getString(Map<String, Object> values, String... keys) {
        if (values == null) {
            return null;
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private static Integer getInteger(Map<String, Object> values, String... keys) {
        if (values == null) {
            return null;
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                try {
                    return Integer.parseInt(value.toString().trim());
                } catch (NumberFormatException ignored) {
                    // Try the next alias.
                }
            }
        }
        return null;
    }

    private static int orDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private SerialPort.Parity parseParity(String parity) {
        if (parity == null) {
            return SerialPort.Parity.NONE;
        }
        return switch (parity.toLowerCase(Locale.ROOT)) {
            case "even" -> SerialPort.Parity.EVEN;
            case "odd" -> SerialPort.Parity.ODD;
            case "mark" -> SerialPort.Parity.MARK;
            case "space" -> SerialPort.Parity.SPACE;
            default -> SerialPort.Parity.NONE;
        };
    }

    public static String busKey(ScaleConfig config) {
        Map<String, Object> params = config.getConnParams();
        String parity = getString(params, "parity");
        return String.join("|",
                String.valueOf(getString(params, "com_port")),
                String.valueOf(orDefault(getInteger(params, "baud_rate"), 9600)),
                String.valueOf(orDefault(getInteger(params, "data_bits"), 8)),
                String.valueOf(orDefault(getInteger(params, "stop_bits"), 1)),
                parity == null ? "none" : parity.toLowerCase(Locale.ROOT));
    }

    public String getBusKey() {
        return busKey;
    }

    public Set<Long> getScaleIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (ScaleConfig config : configs) {
            ids.add(config.getScaleId());
        }
        return ids;
    }

    private void disconnect() {
        ModbusMaster currentMaster = master;
        if (currentMaster == null || !currentMaster.isConnected()) {
            return;
        }
        try {
            currentMaster.disconnect();
            log.info("[RTU Bus {}] Disconnected", busKey);
        } catch (Exception e) {
            log.warn("[RTU Bus {}] Disconnect failed: {}", busKey, e.getMessage());
        }
    }

    @Override
    public void stop() {
        log.warn("[RTU Bus {}] Stop requested (running={}, stopped={}, callerThread={})",
                busKey, running, stopped, Thread.currentThread().getName(),
                new RuntimeException("RTU stop caller"));
        stopped = true;
        disconnect();
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
