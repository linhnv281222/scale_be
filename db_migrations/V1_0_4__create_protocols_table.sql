-- Create protocols table
CREATE TABLE protocols (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    connection_type VARCHAR(50),
    default_port INT,
    default_baud_rate INT,
    is_active BOOLEAN DEFAULT TRUE,
    config_template TEXT,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(255),
    INDEX idx_protocols_code (code),
    INDEX idx_protocols_connection_type (connection_type),
    INDEX idx_protocols_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default protocols
INSERT INTO protocols (code, name, description, connection_type, default_port, default_baud_rate, is_active, config_template, created_by, updated_by)
VALUES 
(
    'MODBUS_TCP',
    'Modbus TCP',
    'Modbus TCP/IP protocol - Communication over Ethernet using TCP',
    'TCP',
    502,
    NULL,
    TRUE,
    '{
        "register_address": 0,
        "num_registers": 2,
        "slave_id": 1,
        "connection_timeout": 5000,
        "read_timeout": 3000,
        "data_format": "FLOAT32",
        "byte_order": "BIG_ENDIAN"
    }',
    'system',
    'system'
),
(
    'MODBUS_RTU',
    'Modbus RTU',
    'Modbus RTU protocol - Serial communication using RS485/RS232',
    'SERIAL',
    NULL,
    9600,
    TRUE,
    '{
        "register_address": 0,
        "num_registers": 2,
        "slave_id": 1,
        "baud_rate": 9600,
        "data_bits": 8,
        "stop_bits": 1,
        "parity": "NONE",
        "connection_timeout": 5000,
        "read_timeout": 3000,
        "data_format": "FLOAT32",
        "byte_order": "BIG_ENDIAN"
    }',
    'system',
    'system'
),
(
    'SBUS',
    'S-Bus',
    'S-Bus protocol - Command-based protocol for industrial scales',
    'SERIAL',
    NULL,
    9600,
    TRUE,
    '{
        "command": "READ_WEIGHT",
        "opcode": "0x57",
        "payload": "",
        "mode": "ASCII",
        "baud_rate": 9600,
        "data_bits": 8,
        "stop_bits": 1,
        "parity": "NONE",
        "connection_timeout": 5000,
        "read_timeout": 3000
    }',
    'system',
    'system'
);

-- Add protocol_id column to scales table (for future linking)
-- ALTER TABLE scales ADD COLUMN protocol_id BIGINT;
-- ALTER TABLE scales ADD CONSTRAINT fk_scales_protocol FOREIGN KEY (protocol_id) REFERENCES protocols(id);
-- CREATE INDEX idx_scales_protocol_id ON scales(protocol_id);
