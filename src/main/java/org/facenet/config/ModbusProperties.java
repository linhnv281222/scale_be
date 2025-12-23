package org.facenet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Modbus protocols
 */
@Configuration
@ConfigurationProperties(prefix = "modbus")
@Data
public class ModbusProperties {

    private Tcp tcp = new Tcp();
    private Rtu rtu = new Rtu();

    @Data
    public static class Tcp {
        private int port = 502;
        private int unitId = 1;
    }

    @Data
    public static class Rtu {
        private int baudRate = 9600;
        private int dataBits = 8;
        private int stopBits = 1;
        private String parity = "NONE";
    }
}
