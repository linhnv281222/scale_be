-- Migration: Add shift_id column to weighing_logs and scale_current_states tables
-- Version: V1.0.9
-- Description: Links weighing data to shifts for automatic shift detection and reporting

-- Add shift_id to weighing_logs table if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'weighing_logs' AND column_name = 'shift_id'
    ) THEN
        ALTER TABLE weighing_logs 
        ADD COLUMN shift_id BIGINT;
        
        ALTER TABLE weighing_logs
        ADD CONSTRAINT fk_weighing_logs_shift
        FOREIGN KEY (shift_id) REFERENCES shifts(id);
        
        CREATE INDEX idx_weighing_logs_shift_id ON weighing_logs(shift_id);
        
        RAISE NOTICE 'Added shift_id column to weighing_logs table';
    ELSE
        RAISE NOTICE 'shift_id column already exists in weighing_logs table';
    END IF;
END $$;

-- Add shift_id to scale_current_states table if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'scale_current_states' AND column_name = 'shift_id'
    ) THEN
        ALTER TABLE scale_current_states 
        ADD COLUMN shift_id BIGINT;
        
        ALTER TABLE scale_current_states
        ADD CONSTRAINT fk_scale_current_states_shift
        FOREIGN KEY (shift_id) REFERENCES shifts(id);
        
        CREATE INDEX idx_scale_current_states_shift_id ON scale_current_states(shift_id);
        
        RAISE NOTICE 'Added shift_id column to scale_current_states table';
    ELSE
        RAISE NOTICE 'shift_id column already exists in scale_current_states table';
    END IF;
END $$;

-- Add comment for documentation
COMMENT ON COLUMN weighing_logs.shift_id IS 'Foreign key to shifts table - automatically detected based on last_time';
COMMENT ON COLUMN scale_current_states.shift_id IS 'Foreign key to shifts table - automatically detected based on last_time';
