package org.facenet.service.scale.engine;

import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.MeasurementEvent;

import java.util.concurrent.BlockingQueue;

/**
 * Factory để tạo ScaleEngine dựa trên protocol
 * Sử dụng Factory Pattern để tách biệt logic tạo engine
 * 
 * Hỗ trợ các protocol:
 * - MODBUS_TCP: Kết nối qua TCP/IP
 * - MODBUS_RTU: Kết nối qua Serial (COM port/RS485)
 * - MODBUS_SBUS: S-Bus protocol (SAIA S-Bus over Serial)
 * - SABUS: Scale-specific ASCII protocol using FL/NT commands
 * - SERIAL: Generic serial ASCII, streaming or request/response
 */
@Slf4j
public class EngineFactory {
    
    /**
     * Tạo engine tương ứng với protocol trong config
     * 
     * @param config Scale configuration từ DB
     * @param queue Active Queue để đẩy dữ liệu vào
     * @return ScaleEngine instance tương ứng
     * @throws IllegalArgumentException nếu protocol không được hỗ trợ
     */
    public static ScaleEngine createEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        if (config == null) {
            throw new IllegalArgumentException("ScaleConfig must not be null");
        }
        String rawProtocol = config.getProtocol();
        if (rawProtocol == null || rawProtocol.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing protocol for scale " + config.getScaleId());
        }

        String protocol = rawProtocol.trim().toUpperCase(java.util.Locale.ROOT);
        
        log.debug("Creating engine for scale {} with protocol {}", config.getScaleId(), protocol);
        
        switch (protocol) {
            case "MODBUS_TCP":
            case "MODBUSTCP":
            case "TCP":
                return new ModbusTcpEngine(config, queue);
                
            case "MODBUS_RTU":
            case "MODBUSRTU":
            case "RTU":
                return new ModbusRtuEngine(config, queue);
                
            case "MODBUS_SBUS":
            case "MODBUSSBUS":
            case "SBUS":
            case "S-BUS":
            case "S_BUS":
                return new SbusEngine(config, queue);

            case "SABUS":
            case "SABUS_ASCII":
                return new SabusAsciiEngine(config, queue);

            case "SIEMENS_S7":
            case "SIEMENS":
            case "S7":
            case "S7_TCP":
                return new SiemensS7Engine(config, queue);
                
            case "SERIAL":
            case "SERIAL_ASCII":
                return new SerialEngine(config, queue);
                
            default:
                throw new IllegalArgumentException("Unsupported protocol: " + protocol + " for scale " + config.getScaleId());
        }
    }
}

