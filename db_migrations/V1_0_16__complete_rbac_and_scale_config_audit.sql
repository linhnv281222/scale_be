CREATE TABLE IF NOT EXISTS scale_config_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    scale_id BIGINT NOT NULL REFERENCES scales(id),
    action VARCHAR(20) NOT NULL,
    before_config JSONB,
    after_config JSONB,
    changed_by VARCHAR(256) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scale_config_audit_scale_time
    ON scale_config_audit_logs(scale_id, changed_at DESC);

UPDATE protocols
SET config_template = jsonb_set(config_template::jsonb, '{conn_params,port}', '102'::jsonb, TRUE)::text,
    default_port = 102,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system'
WHERE code = 'SIEMENS_S7' AND config_template IS NOT NULL;

INSERT INTO permissions (code, description, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    ('DASHBOARD_VIEW', 'View operations dashboard', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('REPORT_VIEW', 'Search and view reports', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('REPORT_EXPORT', 'Export report data', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('CONFIG_MANAGE', 'Manage sensitive device configuration', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('AUDIT_VIEW', 'View configuration audit history', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles (code, name, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    ('SUPERVISOR', 'Supervisor', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('GUEST', 'Guest', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON
    (r.code = 'ADMIN' AND p.code IN ('DASHBOARD_VIEW', 'REPORT_VIEW', 'REPORT_EXPORT', 'CONFIG_MANAGE', 'AUDIT_VIEW', 'SCALE_VIEW')) OR
    (r.code IN ('MANAGER', 'SUPERVISOR') AND p.code IN ('DASHBOARD_VIEW', 'REPORT_VIEW', 'REPORT_EXPORT', 'AUDIT_VIEW', 'SCALE_VIEW')) OR
    (r.code IN ('OPERATOR', 'GUEST') AND p.code IN ('DASHBOARD_VIEW', 'REPORT_VIEW', 'SCALE_VIEW'))
ON CONFLICT (role_id, permission_id) DO NOTHING;
