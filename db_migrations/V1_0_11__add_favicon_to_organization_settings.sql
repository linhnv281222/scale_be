-- Add favicon columns to organization_settings table
ALTER TABLE organization_settings 
ADD COLUMN IF NOT EXISTS favicon_url TEXT,
ADD COLUMN IF NOT EXISTS favicon_data BYTEA;

-- Add comment for new columns
COMMENT ON COLUMN organization_settings.favicon_url IS 'Favicon resource path (relative to classpath)';
COMMENT ON COLUMN organization_settings.favicon_data IS 'Favicon image data stored as binary';
