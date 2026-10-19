-- Migration: Create shift_results table
-- Purpose: Store shift results with start/end values and deviation for data_1

CREATE TABLE shift_results (
    id BIGSERIAL PRIMARY KEY,
    scale_id BIGINT NOT NULL,
    shift_id BIGINT NOT NULL,
    shift_date DATE NOT NULL,
    start_value_data1 VARCHAR(50),
    end_value_data1 VARCHAR(50),
    deviation_data1 DECIMAL(15, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    CONSTRAINT fk_shift_result_scale FOREIGN KEY (scale_id) REFERENCES scales(id) ON DELETE CASCADE,
    CONSTRAINT fk_shift_result_shift FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE CASCADE,
    CONSTRAINT uk_shift_result_scale_shift_date UNIQUE (scale_id, shift_id, shift_date)
);

-- Create indexes for better query performance
CREATE INDEX idx_shift_results_scale_id ON shift_results(scale_id);
CREATE INDEX idx_shift_results_shift_id ON shift_results(shift_id);
CREATE INDEX idx_shift_results_shift_date ON shift_results(shift_date);
CREATE INDEX idx_shift_results_scale_shift_date ON shift_results(scale_id, shift_date);

-- Add comments
COMMENT ON TABLE shift_results IS 'Store shift results with start/end values and deviation';
COMMENT ON COLUMN shift_results.scale_id IS 'Reference to scale';
COMMENT ON COLUMN shift_results.shift_id IS 'Reference to shift';
COMMENT ON COLUMN shift_results.shift_date IS 'Date of the shift';
COMMENT ON COLUMN shift_results.start_value_data1 IS 'Starting value of data_1 at shift start';
COMMENT ON COLUMN shift_results.end_value_data1 IS 'Ending value of data_1 at shift end';
COMMENT ON COLUMN shift_results.deviation_data1 IS 'Deviation (end - start) of data_1';
