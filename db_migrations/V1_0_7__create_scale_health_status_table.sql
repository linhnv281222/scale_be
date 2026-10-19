-- Migration: Create scale_health_status table for monitoring
-- Version: V1_0_7
-- Description: Add scale health monitoring and alert tracking

CREATE TABLE IF NOT EXISTS scale_health_status (
    id BIGSERIAL PRIMARY KEY,
    scale_id BIGINT NOT NULL,
    health_status VARCHAR(50) NOT NULL,
    issue_type VARCHAR(50),
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    duration_seconds BIGINT,
    is_active_issue BOOLEAN NOT NULL DEFAULT true,
    last_known_value TEXT,
    error_message TEXT,
    consecutive_failures INTEGER DEFAULT 0,
    metadata TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    
    -- Foreign key
    CONSTRAINT fk_health_scale FOREIGN KEY (scale_id) REFERENCES scales(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_health_scale_id ON scale_health_status(scale_id);
CREATE INDEX idx_health_status ON scale_health_status(health_status);
CREATE INDEX idx_health_detected_at ON scale_health_status(detected_at);
CREATE INDEX idx_health_is_active ON scale_health_status(is_active_issue);
CREATE INDEX idx_health_scale_active ON scale_health_status(scale_id, is_active_issue);

-- Comments
COMMENT ON TABLE scale_health_status IS 'Tracks scale health status and connectivity issues for monitoring';
COMMENT ON COLUMN scale_health_status.health_status IS 'Health status: HEALTHY, STOPPED, NO_DATA, CONNECTION_LOST, READ_ERROR, DEGRADED';
COMMENT ON COLUMN scale_health_status.issue_type IS 'Issue type: ZERO_VALUE, NO_SIGNAL, TIMEOUT, PROTOCOL_ERROR, HARDWARE_ERROR, NETWORK_ERROR, UNKNOWN';
COMMENT ON COLUMN scale_health_status.detected_at IS 'When the issue was first detected';
COMMENT ON COLUMN scale_health_status.resolved_at IS 'When the issue was resolved (null if still active)';
COMMENT ON COLUMN scale_health_status.is_active_issue IS 'Whether this is an active/ongoing issue';
COMMENT ON COLUMN scale_health_status.consecutive_failures IS 'Number of consecutive health check failures';
