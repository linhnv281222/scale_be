-- Migration V1.0.12: Add is_active column to RBAC and Location tables for soft delete support
-- Date: 2026-01-12

-- Add is_active to locations table
ALTER TABLE locations ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_locations_is_active ON locations(is_active);
COMMENT ON COLUMN locations.is_active IS 'Indicates if the location is active (soft delete flag)';

-- Add is_active to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);
COMMENT ON COLUMN users.is_active IS 'Indicates if the user is active (soft delete flag)';

-- Add is_active to roles table
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_roles_is_active ON roles(is_active);
COMMENT ON COLUMN roles.is_active IS 'Indicates if the role is active (soft delete flag)';

-- Add is_active to permissions table
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_permissions_is_active ON permissions(is_active);
COMMENT ON COLUMN permissions.is_active IS 'Indicates if the permission is active (soft delete flag)';

-- Update existing records to have is_active = TRUE (if the column was newly added)
UPDATE locations SET is_active = TRUE WHERE is_active IS NULL;
UPDATE users SET is_active = TRUE WHERE is_active IS NULL;
UPDATE roles SET is_active = TRUE WHERE is_active IS NULL;
UPDATE permissions SET is_active = TRUE WHERE is_active IS NULL;
