-- Add direction column to scales table
ALTER TABLE scales ADD COLUMN direction VARCHAR(20) AFTER type;

-- Add comment for clarity
ALTER TABLE scales MODIFY COLUMN direction VARCHAR(20) COMMENT 'Scale direction: IMPORT (Nhập) or EXPORT (Xuất)';

-- Optional: Update existing scales with default values
-- UPDATE scales SET direction = 'IMPORT' WHERE direction IS NULL;
