-- Migration: Add template_type column to template_imports table
-- Version: 1.0.6
-- Date: 2026-01-06
-- Description: Add template_type field to categorize templates as "Báo cáo ca" or "Báo cáo cân"

-- Add template_type column
ALTER TABLE template_imports
ADD COLUMN template_type VARCHAR(50);

-- Add comment
COMMENT ON COLUMN template_imports.template_type IS 'Template type: SHIFT_REPORT (Báo cáo ca) or WEIGHING_REPORT (Báo cáo cân)';

-- Create index for better query performance
CREATE INDEX idx_template_imports_type ON template_imports(template_type);

-- Optional: Update existing records with default value if needed
-- UPDATE template_imports SET template_type = 'WEIGHING_REPORT' WHERE template_type IS NULL;
