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
    
    private final ScaleConfig config;
    private final BlockingQueue<MeasurementEvent> queue;
    private volatile boolean stopped = false;
    private volatile boolean running = false;
    
    private ModbusMaster master;
    
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
            
            if (ip == null || port == null) {
                log.error("[Engine {}] Missing IP or Port in config", config.getScaleId());
                return;
            }
            
            // 2. Thiết lập TCP Parameters
            tcpParameters.setHost(InetAddress.getByName(ip));
            tcpParameters.setPort(port);
            tcpParameters.setKeepAlive(true);
            
            // 3. Tạo Modbus Master
            master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);
            master.setResponseTimeout(2000); // Timeout 2 giây
            
            log.info("[Engine {}] Connecting to {}:{}...", config.getScaleId(), ip, port);
            
            // 4. Vòng lặp đọc dữ liệu
            while (!stopped) {
                try {
                    // Kết nối nếu chưa connected
                    if (!master.isConnected()) {
                        master.connect();
                        log.info("[Engine {}] Connected to {}:{}", config.getScaleId(), ip, port);
                    }
                    
                    // Tạo MeasurementEvent
                    MeasurementEvent event = MeasurementEvent.builder()
                            .scaleId(config.getScaleId())
                            .lastTime(ZonedDateTime.now())
                            .status("ONLINE")
                            .build();
                    
                    // Lấy Unit ID (Slave ID)
                    Integer unitId = getConnParamAsInt("unit_id");
                    if (unitId == null) {
                        unitId = 1; // Default Unit ID
                    }
                    
                    // Đọc data_1 -> data_5 và tạo DataField object
                    event.setData1(createDataField(master, unitId, config.getData1()));
                    event.setData2(createDataField(master, unitId, config.getData2()));
                    event.setData3(createDataField(master, unitId, config.getData3()));
                    event.setData4(createDataField(master, unitId, config.getData4()));
                    event.setData5(createDataField(master, unitId, config.getData5()));
                    
                    // Đẩy vào Queue
                    queue.put(event);
                    log.debug("[Engine {}] Pushed measurement to queue", config.getScaleId());
                    
                } catch (InterruptedException e) {
                    log.warn("[Engine {}] Interrupted, stopping...", config.getScaleId());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // QUAN TRỌNG: Không để exception kill thread
                    log.error("[Engine {}] Error reading scale: {}", config.getScaleId(), e.getMessage());
                    
                    // Đánh dấu offline và retry sau 5s
                    try {
                        // Ngắt kết nối và thử kết nối lại
                        if (master != null && master.isConnected()) {
                            master.disconnect();
                        }
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
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
    private String readRegister(ModbusMaster master, int unitId, Map<String, Object> dataConfig) {
        if (dataConfig == null || !isDataSlotUsed(dataConfig)) {
            return null;
        }
        
        try {
            // Lấy thông tin thanh ghi từ config (hỗ trợ nhiều format)
            Integer startAddress = getDataConfigParam(dataConfig, "start_registers");
            if (startAddress == null) {
                startAddress = getDataConfigParam(dataConfig, "register_start"); // Fallback
            }
            
            Integer numRegisters = getDataConfigParam(dataConfig, "num_registers");
            if (numRegisters == null) {
                numRegisters = getDataConfigParam(dataConfig, "register_count"); // Fallback
            }
            
            if (startAddress == null || numRegisters == null) {
                log.warn("[Engine {}] Invalid register config", config.getScaleId());
                return null;
            }
            
            // Đọc Holding Registers
            int[] registers = master.readHoldingRegisters(unitId, startAddress, numRegisters);
            
            // Lấy data_type từ config
            String dataType = getDataConfigParam(dataConfig, "data_type");
            
            // Convert sang String với data_type tương ứng
            return ModbusDataConverter.registersToString(registers, dataType);
            
        } catch (Exception e) {
            log.error("[Engine {}] Error reading register: {}", config.getScaleId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Tạo DataField object từ config và giá trị đọc được
     */
    private DataField createDataField(ModbusMaster master, int unitId, Map<String, Object> dataConfig) {
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
    
    /**
     * Lấy tham số từ data config
     */
    @SuppressWarnings("unchecked")
    private <T> T getDataConfigParam(Map<String, Object> dataConfig, String key) {
        return (T) dataConfig.get(key);
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
