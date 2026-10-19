-- Migration: Add protocol_id to scales table
-- Date: 2024-01-XX
-- Description: Add protocol_id foreign key reference to scales table

-- Add protocol_id column
ALTER TABLE scales ADD COLUMN IF NOT EXISTS protocol_id BIGINT;

-- Add foreign key constraint
ALTER TABLE scales ADD CONSTRAINT fk_scales_protocol 
    FOREIGN KEY (protocol_id) REFERENCES protocols(id);

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_scales_protocol ON scales(protocol_id);

-- Comment for documentation
COMMENT ON COLUMN scales.protocol_id IS 'Reference to the protocol used by this scale';
