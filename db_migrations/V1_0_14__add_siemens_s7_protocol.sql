-- Register Siemens S7 in the protocol catalog used by scale configuration screens.
INSERT INTO protocols (
    code, name, description, connection_type, default_port,
    default_baud_rate, is_active, config_template, created_by, updated_by
)
SELECT
    'SIEMENS_S7',
    'Siemens S7',
    'Siemens S7 communication over ISO-on-TCP using Apache PLC4X',
    'TCP',
    102,
    NULL,
    TRUE,
    '{
        "conn_params": {
            "ip": "192.168.1.20",
            "rack": 0,
            "slot": 1,
            "controller_type": "S7_1200",
            "connect_timeout_ms": 5000,
            "read_timeout_ms": 5000
        },
        "data": {
            "name": "Weight",
            "tag_address": "%DB1:0:REAL",
            "is_used": true
        }
    }',
    'system',
    'system'
WHERE NOT EXISTS (SELECT 1 FROM protocols WHERE code = 'SIEMENS_S7');
