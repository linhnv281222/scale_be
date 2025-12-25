-- Add description column to locations table
-- Run this script after the LocationManagementPage UI updates

ALTER TABLE locations
ADD COLUMN IF NOT EXISTS description VARCHAR(255);

COMMENT ON COLUMN locations.description IS 'Mô tả vị trí';
