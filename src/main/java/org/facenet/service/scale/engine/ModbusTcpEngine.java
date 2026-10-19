package org.facenet.service.scale.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.DataField;
import org.facenet.event.MeasurementEvent;
import org.facenet.service.scale.engine.util.ModbusDataConverter;
import org.facenet.service.scale.engine.util.ConnectionFailureTracker;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Modbus TCP Engine - Đọc dữ liệu THẬT từ cân qua giao thức Modbus TCP
 * 
 * Phiên bản 3.1: Sử dụng thư viện jlibmodbus để kết nối và đọc dữ liệu thực từ thiết bị
 * 
 * Luồng hoạt động:
 * 1. Lấy Config từ DB (IP, Port, Unit ID)
 * 2. Loop: Chạy vòng lặp while(!stopped)
 * 3. Connect: Kết nối tới Modbus TCP Master
 * 4. Read: Đọc thanh ghi (Holding Registers) theo cấu hình data_1 -> data_5
 * 5. Convert: Chuyển đổi registers sang String bằng ModbusDataConverter
 * 6. Push: Đẩy MeasurementEvent vào Queue
 * 7. Sleep: Nghỉ theo poll_interval
 */
@Slf4j
public class ModbusTcpEngine implements ScaleEngine {

    private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 5000;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final long OFFLINE_THRESHOLD_MS = 60_000L;
    
    private final ScaleConfig config;
    private final BlockingQueue<MeasurementEvent> queue;
    private volatile boolean stopped = false;
    private volatile boolean running = false;
    
    private ModbusMaster master;
    private final ConnectionFailureTracker failureTracker = new ConnectionFailureTracker();
    
    public ModbusTcpEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        this.config = config;
        this.queue = queue;
    }
    
    @Override
    public void run() {
        running = true;
        int pollIntervalMs = getPollIntervalMs();
        log.info("[Engine {}] Modbus TCP Engine started with pollInterval={}ms", 
            config.getScaleId(), pollIntervalMs);
        
        TcpParameters tcpParameters = new TcpParameters();
        
        try {
            // 1. Lấy thông tin kết nối từ config
            String ip = getConnParamAsString("ip");
            Integer port = getConnParamAsInt("port");
            Integer deviceId = getConnParamAsInt("id"); // Lấy ID từ connection_params
            
            if (ip == null || port == null) {
                log.error("[Engine {}] Missing IP or Port in config", config.getScaleId());
                return;
            }
            
            // Log thông tin ID nếu có
            if (deviceId != null) {
                log.info("[Engine {}] Device ID from connection_params: {}", config.getScaleId(), deviceId);
            }
            
            // 2. Thiết lập TCP Parameters
            tcpParameters.setHost(InetAddress.getByName(ip));
            tcpParameters.setPort(port);
            tcpParameters.setKeepAlive(true);
            
            // 3. Tạo Modbus Master
            master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);
            Integer responseTimeout = getFirstConnParamAsInt("response_timeout_ms", "read_timeout_ms");
            master.setResponseTimeout(responseTimeout != null
                    ? responseTimeout : DEFAULT_RESPONSE_TIMEOUT_MS);
            
            if (deviceId != null) {
                log.info("[Engine {}] Connecting to {}:{} with Device ID: {}...", config.getScaleId(), ip, port, deviceId);
            } else {
                log.info("[Engine {}] Connecting to {}:{}...", config.getScaleId(), ip, port);
            }
            
            // 4. Vòng lặp đọc dữ liệu
            while (!stopped) {
                try {
                    // Kết nối nếu chưa connected
                    if (!master.isConnected()) {
                        master.connect();
                        if (deviceId != null) {
                            log.info("[Engine {}] Connected to {}:{} (Device ID: {})", config.getScaleId(), ip, port, deviceId);
                        } else {
                            log.info("[Engine {}] Connected to {}:{}", config.getScaleId(), ip, port);
                        }
                    }
                    
                    // Lấy Unit ID (Slave ID)
                    Integer unitId = getFirstConnParamAsInt("unit_id", "device_id", "slave_id", "id");
                    if (unitId == null) {
                        unitId = 1; // Default Unit ID
                    }
                    
                    MeasurementEvent event = readMeasurement(master, unitId);
                    recordSuccessfulRead();
                    
                    // Đẩy vào Queue
                    queue.put(event);
                    log.debug("[Engine {}] Pushed measurement to queue", config.getScaleId());
                    
                } catch (InterruptedException e) {
                    log.warn("[Engine {}] Interrupted, stopping...", config.getScaleId());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("[Engine {}] Error reading scale: {}", config.getScaleId(), e.getMessage());

                    publishOfflineIfDue(e);
                    try {
                        if (master != null && master.isConnected()) {
                            master.disconnect();
                        }
                        Thread.sleep(RECONNECT_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception disconnectError) {
                        log.warn("[Engine {}] Error disconnecting failed connection: {}",
                                config.getScaleId(), disconnectError.getMessage());
                    }
                    continue;
                }
                
                // Nghỉ theo poll_interval
                int pollInterval = pollIntervalMs;
                log.trace("[Engine {}] Sleeping for {}ms (pollInterval)", config.getScaleId(), pollInterval);
                Thread.sleep(pollInterval);
            }
            
        } catch (Exception e) {
            log.error("[Engine {}] Fatal error in TCP Engine: {}", config.getScaleId(), e.getMessage(), e);
        } finally {
            // Cleanup: Đóng kết nối
            cleanup();
        }
        
        running = false;
        log.info("[Engine {}] Modbus TCP Engine stopped", config.getScaleId());
    }
    
    /**
     * Đọc thanh ghi Modbus và trả về dạng String
     * @param master Modbus Master instance
     * @param unitId Unit ID (Slave ID)
     * @param dataConfig Cấu hình data slot (data_1, data_2, ...)
     * @return String value hoặc null nếu không đọc được
     */
    private MeasurementEvent readMeasurement(ModbusMaster master, int unitId) throws Exception {
        if (!hasConfiguredDataField()) {
            throw new IllegalStateException("No enabled data field is configured");
        }

        DataField data1 = createDataField(master, unitId, config.getData1());
        DataField data2 = createDataField(master, unitId, config.getData2());
        DataField data3 = createDataField(master, unitId, config.getData3());
        DataField data4 = createDataField(master, unitId, config.getData4());
        DataField data5 = createDataField(master, unitId, config.getData5());

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

    private String readRegister(ModbusMaster master, int unitId, Map<String, Object> dataConfig) throws Exception {
        if (dataConfig == null || !isDataSlotUsed(dataConfig)) {
            return null;
        }

        Integer startAddress = getIntegerDataConfigParam(dataConfig,
                "start_registers", "start_register", "register_start");
        Integer numRegisters = getIntegerDataConfigParam(dataConfig,
                "num_registers", "register_count");

        if (startAddress == null || numRegisters == null || numRegisters <= 0) {
            throw new IllegalArgumentException("Invalid register config for scale " + config.getScaleId());
        }

        int[] registers = readRegisters(master, unitId, startAddress, numRegisters, dataConfig);
        String value = ModbusDataConverter.registersToString(registers, dataConfig);
        if (value == null) {
            throw new IllegalStateException("Unable to convert register value for scale " + config.getScaleId());
        }
        return value;
    }

    private int[] readRegisters(ModbusMaster master, int unitId, int startAddress,
                                int numRegisters, Map<String, Object> dataConfig) throws Exception {
        Integer functionCode = getIntegerDataConfigParam(dataConfig, "function_code", "function");
        int code = functionCode != null ? functionCode : 3;
        return switch (code) {
            case 3 -> master.readHoldingRegisters(unitId, startAddress, numRegisters);
            case 4 -> master.readInputRegisters(unitId, startAddress, numRegisters);
            default -> throw new IllegalArgumentException("Unsupported Modbus function_code " + code
                    + "; register fields support function 3 or 4");
        };
    }
    
    /**
     * Tạo DataField object từ config và giá trị đọc được
     */
    private DataField createDataField(ModbusMaster master, int unitId, Map<String, Object> dataConfig) throws Exception {
        if (dataConfig == null || !isDataSlotUsed(dataConfig)) {
            return null;
        }
        
        String name = getDataName(dataConfig);
        String value = readRegister(master, unitId, dataConfig);
        
        if (value == null) {
            return null;
        }
        
        return DataField.builder()
                .name(name)
                .value(value)
                .build();
    }

    private boolean hasConfiguredDataField() {
        return isDataSlotUsed(config.getData1()) || isDataSlotUsed(config.getData2())
                || isDataSlotUsed(config.getData3()) || isDataSlotUsed(config.getData4())
                || isDataSlotUsed(config.getData5());
    }

    private void recordSuccessfulRead() {
        failureTracker.recordSuccess();
    }

    private void publishOfflineIfDue(Exception cause) {
        long now = System.currentTimeMillis();
        if (failureTracker.shouldPublishOffline(now, OFFLINE_THRESHOLD_MS)) {
            MeasurementEvent offlineEvent = MeasurementEvent.builder()
                    .scaleId(config.getScaleId())
                    .lastTime(ZonedDateTime.now())
                    .status("OFFLINE")
                    .build();
            if (queue.offer(offlineEvent)) {
                failureTracker.markOfflinePublished();
                log.warn("[Engine {}] Marked OFFLINE after a continuous read failure: {}",
                        config.getScaleId(), cause.getMessage());
            } else {
                log.error("[Engine {}] Cannot publish OFFLINE event because measurement queue is full",
                        config.getScaleId());
            }
        }
    }

    /**
     * Lấy tên của data field từ config
     */
    private String getDataName(Map<String, Object> dataConfig) {
        if (dataConfig == null) {
            return null;
        }
        Object name = dataConfig.get("name");
        return name != null ? name.toString() : null;
    }

    /**
     * Kiểm tra data slot có được sử dụng không
     */
    private boolean isDataSlotUsed(Map<String, Object> dataConfig) {
        Object used = dataConfig.get("used");
        if (used == null) {
            used = dataConfig.get("is_used"); // Support cả 2 format
        }
        return toBoolean(used);
    }

    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        String s = value.toString().trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return false;
        return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y");
    }

    private int getPollIntervalMs() {
        Integer pollInterval = config.getPollInterval();
        if (pollInterval == null || pollInterval <= 0) {
            return 1000;
        }
        return pollInterval;
    }
    
    /**
     * Lấy tham số từ connParams (hỗ trợ cả Map và JsonNode)
     */
    private String getConnParamAsString(String key) {
        Object connParams = config.getConnParams();
        if (connParams instanceof Map) {
            Object value = ((Map<?, ?>) connParams).get(key);
            return value != null ? value.toString() : null;
        } else if (connParams instanceof JsonNode) {
            JsonNode node = ((JsonNode) connParams).get(key);
            if (node == null) return null;
            if (node.isTextual()) return node.asText();
            if (node.isNumber()) return node.asText();
            return node.toString();
        }
        return null;
    }

    private Integer getConnParamAsInt(String key) {
        Object connParams = config.getConnParams();
        Object value = null;
        if (connParams instanceof Map) {
            value = ((Map<?, ?>) connParams).get(key);
        } else if (connParams instanceof JsonNode) {
            JsonNode node = ((JsonNode) connParams).get(key);
            if (node == null) return null;
            if (node.isInt() || node.isLong() || node.isShort()) return node.asInt();
            if (node.isNumber()) return node.asInt();
            if (node.isTextual()) value = node.asText();
            else return null;
        }
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer getFirstConnParamAsInt(String... keys) {
        for (String key : keys) {
            Integer value = getConnParamAsInt(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
    
    /**
     * Lấy tham số từ data config
     */
    @SuppressWarnings("unchecked")
    private <T> T getDataConfigParam(Map<String, Object> dataConfig, String key) {
        return (T) dataConfig.get(key);
    }

    private Integer getIntegerDataConfigParam(Map<String, Object> dataConfig, String... keys) {
        for (String key : keys) {
            Object value = dataConfig.get(key);
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
    
    /**
     * Cleanup resources
     */
    private void cleanup() {
        try {
            if (master != null && master.isConnected()) {
                master.disconnect();
                log.info("[Engine {}] Disconnected from Modbus TCP", config.getScaleId());
            }
        } catch (Exception e) {
            log.error("[Engine {}] Error during cleanup: {}", config.getScaleId(), e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        log.info("[Engine {}] Stop requested", config.getScaleId());
        stopped = true;
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
