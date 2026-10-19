-- Create organization_settings table
CREATE TABLE IF NOT EXISTS organization_settings (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    company_name_en VARCHAR(255),
    address TEXT,
    phone VARCHAR(50),
    email VARCHAR(100),
    website VARCHAR(255),
    tax_code VARCHAR(50),
    logo_url TEXT,
    logo_data BYTEA,
    watermark_text VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Create index for default organization
CREATE INDEX IF NOT EXISTS idx_organization_settings_default ON organization_settings(is_default) WHERE is_default = true;

-- Insert default organization if not exists
INSERT INTO organization_settings (company_name, is_active, is_default)
SELECT 'CÔNG TY MẪU', true, true
WHERE NOT EXISTS (SELECT 1 FROM organization_settings WHERE is_default = true);
