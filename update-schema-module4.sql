-- Update schema for Module 4: Persistence
-- Change data columns from varchar to jsonb for scale_current_states and weighing_logs

-- Update scale_current_states table
ALTER TABLE scale_current_states
ALTER COLUMN data_1 TYPE jsonb USING CASE WHEN data_1 IS NULL THEN NULL ELSE jsonb_build_object('val', data_1) END,
ALTER COLUMN data_2 TYPE jsonb USING CASE WHEN data_2 IS NULL THEN NULL ELSE jsonb_build_object('val', data_2) END,
ALTER COLUMN data_3 TYPE jsonb USING CASE WHEN data_3 IS NULL THEN NULL ELSE jsonb_build_object('val', data_3) END,
ALTER COLUMN data_4 TYPE jsonb USING CASE WHEN data_4 IS NULL THEN NULL ELSE jsonb_build_object('val', data_4) END,
ALTER COLUMN data_5 TYPE jsonb USING CASE WHEN data_5 IS NULL THEN NULL ELSE jsonb_build_object('val', data_5) END;

-- Update weighing_logs table
ALTER TABLE weighing_logs
ALTER COLUMN data_1 TYPE jsonb USING CASE WHEN data_1 IS NULL THEN NULL ELSE jsonb_build_object('val', data_1) END,
ALTER COLUMN data_2 TYPE jsonb USING CASE WHEN data_2 IS NULL THEN NULL ELSE jsonb_build_object('val', data_2) END,
ALTER COLUMN data_3 TYPE jsonb USING CASE WHEN data_3 IS NULL THEN NULL ELSE jsonb_build_object('val', data_3) END,
ALTER COLUMN data_4 TYPE jsonb USING CASE WHEN data_4 IS NULL THEN NULL ELSE jsonb_build_object('val', data_4) END,
ALTER COLUMN data_5 TYPE jsonb USING CASE WHEN data_5 IS NULL THEN NULL ELSE jsonb_build_object('val', data_5) END;

-- Add comment for jsonb columns
COMMENT ON COLUMN scale_current_states.data_1 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN scale_current_states.data_2 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN scale_current_states.data_3 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN scale_current_states.data_4 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN scale_current_states.data_5 IS 'JSONB format: {"val": "measured_value"}';

COMMENT ON COLUMN weighing_logs.data_1 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN weighing_logs.data_2 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN weighing_logs.data_3 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN weighing_logs.data_4 IS 'JSONB format: {"val": "measured_value"}';
COMMENT ON COLUMN weighing_logs.data_5 IS 'JSONB format: {"val": "measured_value"}';