-- Migration: Create template_imports table for tracking imported templates
-- Date: 2025-12-31
-- Description: Stores metadata about imported template files and their resource locations

CREATE TABLE IF NOT EXISTS template_imports (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL UNIQUE,
    template_code VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    resource_path VARCHAR(500) NOT NULL UNIQUE,
    file_path VARCHAR(1000) NOT NULL,
    file_size_bytes BIGINT,
    file_hash VARCHAR(64),
    import_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    import_date TIMESTAMP WITH TIME ZONE NOT NULL,
    import_notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit columns (from Auditable)
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_template_imports_template_id 
        FOREIGN KEY (template_id) REFERENCES report_templates(id) ON DELETE CASCADE
);

-- Create indices for better query performance
CREATE INDEX idx_template_code ON template_imports(template_code);
CREATE INDEX idx_import_status ON template_imports(import_status);
CREATE INDEX idx_template_import_id ON template_imports(template_id);
CREATE INDEX idx_file_hash ON template_imports(file_hash);
CREATE INDEX idx_resource_path ON template_imports(resource_path);
CREATE INDEX idx_is_active ON template_imports(is_active);

-- Add comment to table
COMMENT ON TABLE template_imports IS 'Tracks imported template files with metadata and resource location';
COMMENT ON COLUMN template_imports.resource_path IS 'Relative path in resources directory (e.g., templates/reports/my-template.docx)';
COMMENT ON COLUMN template_imports.file_path IS 'Absolute file system path where template is stored';
COMMENT ON COLUMN template_imports.file_hash IS 'SHA-256 hash for integrity verification and duplicate detection';
COMMENT ON COLUMN template_imports.import_status IS 'Status: PENDING, ACTIVE, ARCHIVED, CORRUPTED, DELETED';
