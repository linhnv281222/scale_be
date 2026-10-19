INSERT INTO protocols (
    code, name, description, connection_type, default_port,
    default_baud_rate, is_active, config_template, created_by, updated_by
)
VALUES (
    'SERIAL',
    'Generic Serial ASCII',
    'Configurable serial ASCII protocol for streaming and request/response scales',
    'SERIAL',
    NULL,
    9600,
    TRUE,
    '{
        "conn_params": {
            "com_port": "COM1",
            "baud_rate": 9600,
            "data_bits": 8,
            "stop_bits": 1,
            "parity": "NONE",
            "charset": "US-ASCII",
            "response_timeout_ms": 3000,
            "reconnect_delay_ms": 5000,
            "offline_threshold_ms": 60000,
            "command_prefix": "",
            "command_terminator": "\\r",
            "response_terminator": "\\r",
            "strip_echo": true,
            "max_response_bytes": 1024
        },
        "data_1": {
            "name": "Weight",
            "command": "W",
            "response_pattern": "([+-]?[0-9]+(?:[.,][0-9]+)?)",
            "value_type": "NUMBER",
            "is_used": true
        }
    }',
    'system',
    'system'
)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    connection_type = EXCLUDED.connection_type,
    default_baud_rate = EXCLUDED.default_baud_rate,
    is_active = EXCLUDED.is_active,
    config_template = EXCLUDED.config_template,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system';
