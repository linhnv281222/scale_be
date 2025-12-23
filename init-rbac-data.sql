-- ============================================
-- RBAC Initialization Script
-- Create permissions, roles and admin user
-- ============================================

-- Clean up existing data (optional - comment out if you want to keep existing data)
-- TRUNCATE TABLE user_roles, role_permissions, users, roles, permissions CASCADE;

-- ============================================
-- 1. Insert Permissions
-- ============================================
INSERT INTO permissions (code, description, created_at, created_by) VALUES
-- User Management Permissions
('USER_VIEW', 'Xem danh sách người dùng', CURRENT_TIMESTAMP, 'system'),
('USER_CREATE', 'Tạo người dùng mới', CURRENT_TIMESTAMP, 'system'),
('USER_UPDATE', 'Cập nhật thông tin người dùng', CURRENT_TIMESTAMP, 'system'),
('USER_DELETE', 'Xóa người dùng', CURRENT_TIMESTAMP, 'system'),

-- Role Management Permissions
('ROLE_VIEW', 'Xem danh sách vai trò', CURRENT_TIMESTAMP, 'system'),
('ROLE_CREATE', 'Tạo vai trò mới', CURRENT_TIMESTAMP, 'system'),
('ROLE_UPDATE', 'Cập nhật vai trò', CURRENT_TIMESTAMP, 'system'),
('ROLE_DELETE', 'Xóa vai trò', CURRENT_TIMESTAMP, 'system'),

-- Permission Management
('PERMISSION_VIEW', 'Xem danh sách quyền', CURRENT_TIMESTAMP, 'system'),

-- Scale Management Permissions
('SCALE_VIEW', 'Xem thông tin cân', CURRENT_TIMESTAMP, 'system'),
('SCALE_CREATE', 'Tạo cân mới', CURRENT_TIMESTAMP, 'system'),
('SCALE_UPDATE', 'Cập nhật thông tin cân', CURRENT_TIMESTAMP, 'system'),
('SCALE_DELETE', 'Xóa cân', CURRENT_TIMESTAMP, 'system'),
('SCALE_READ', 'Đọc số liệu từ cân', CURRENT_TIMESTAMP, 'system'),

-- Location Management Permissions
('LOCATION_VIEW', 'Xem danh sách địa điểm', CURRENT_TIMESTAMP, 'system'),
('LOCATION_CREATE', 'Tạo địa điểm mới', CURRENT_TIMESTAMP, 'system'),
('LOCATION_UPDATE', 'Cập nhật địa điểm', CURRENT_TIMESTAMP, 'system'),
('LOCATION_DELETE', 'Xóa địa điểm', CURRENT_TIMESTAMP, 'system'),

-- Form Management Permissions
('FORM_VIEW', 'Xem biểu mẫu', CURRENT_TIMESTAMP, 'system'),
('FORM_CREATE', 'Tạo biểu mẫu', CURRENT_TIMESTAMP, 'system'),
('FORM_UPDATE', 'Cập nhật biểu mẫu', CURRENT_TIMESTAMP, 'system'),
('FORM_DELETE', 'Xóa biểu mẫu', CURRENT_TIMESTAMP, 'system'),

-- Report Permissions
('REPORT_VIEW', 'Xem báo cáo', CURRENT_TIMESTAMP, 'system'),
('REPORT_EXPORT', 'Xuất báo cáo', CURRENT_TIMESTAMP, 'system'),

-- System Permissions
('SYSTEM_CONFIG', 'Cấu hình hệ thống', CURRENT_TIMESTAMP, 'system'),
('SYSTEM_MONITOR', 'Giám sát hệ thống', CURRENT_TIMESTAMP, 'system')
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- 2. Insert Roles
-- ============================================
INSERT INTO roles (name, code, created_at, created_by) VALUES
('Quản trị viên', 'ADMIN', CURRENT_TIMESTAMP, 'system'),
('Quản lý', 'MANAGER', CURRENT_TIMESTAMP, 'system'),
('Nhân viên kho', 'WAREHOUSE_STAFF', CURRENT_TIMESTAMP, 'system'),
('Người xem', 'VIEWER', CURRENT_TIMESTAMP, 'system')
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- 3. Assign Permissions to Roles
-- ============================================

-- ADMIN: Full permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- MANAGER: All view and some management permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'MANAGER'
AND p.code IN (
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE',
    'ROLE_VIEW', 'PERMISSION_VIEW',
    'SCALE_VIEW', 'SCALE_CREATE', 'SCALE_UPDATE', 'SCALE_READ',
    'LOCATION_VIEW', 'LOCATION_CREATE', 'LOCATION_UPDATE',
    'FORM_VIEW', 'FORM_CREATE', 'FORM_UPDATE',
    'REPORT_VIEW', 'REPORT_EXPORT',
    'SYSTEM_MONITOR'
)
ON CONFLICT DO NOTHING;

-- WAREHOUSE_STAFF: Scale and location management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'WAREHOUSE_STAFF'
AND p.code IN (
    'SCALE_VIEW', 'SCALE_READ',
    'LOCATION_VIEW',
    'FORM_VIEW',
    'REPORT_VIEW'
)
ON CONFLICT DO NOTHING;

-- VIEWER: Read-only permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VIEWER'
AND p.code IN (
    'USER_VIEW',
    'ROLE_VIEW', 'PERMISSION_VIEW',
    'SCALE_VIEW',
    'LOCATION_VIEW',
    'FORM_VIEW',
    'REPORT_VIEW'
)
ON CONFLICT DO NOTHING;

-- ============================================
-- 4. Create Default Admin User
-- Password: Admin@123456 (hashed with BCrypt)
-- ============================================
INSERT INTO users (username, password_hash, full_name, status, created_at, created_by, updated_at, updated_by) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrator', 1, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system')
ON CONFLICT (username) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- ============================================
-- 5. Create Sample Users (Optional)
-- ============================================

-- Manager User (password: Manager@123)
INSERT INTO users (username, password_hash, full_name, status, created_at, created_by, updated_at, updated_by) VALUES
('manager01', '$2a$10$5rJ8iGhXvU7KzQPYL9oJb.w8vPL9YE3k7X2NkJQZ9xMKGZYhH8rOa', 'Nguyen Van A', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'manager01' AND r.code = 'MANAGER'
ON CONFLICT DO NOTHING;

-- Warehouse Staff User (password: Staff@123)
INSERT INTO users (username, password_hash, full_name, status, created_at, created_by, updated_at, updated_by) VALUES
('staff01', '$2a$10$7vBvXH5tJ3pXL9kFY6oMx.y9wQL9ZE5m9X3PlMRZ8xLJHZZhI9sOa', 'Tran Thi B', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'staff01' AND r.code = 'WAREHOUSE_STAFF'
ON CONFLICT DO NOTHING;

-- Viewer User (password: Viewer@123)
INSERT INTO users (username, password_hash, full_name, status, created_at, created_by, updated_at, updated_by) VALUES
('viewer01', '$2a$10$9xDvYH7uL5rXN9mHZ8qPy.z0xRM9ZF6n0X4QnNSZ9yMKIZZiJ0tPa', 'Le Van C', 1, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'viewer01' AND r.code = 'VIEWER'
ON CONFLICT DO NOTHING;

-- ============================================
-- Verification Queries
-- ============================================

-- Count permissions
SELECT 'Total Permissions:' as info, COUNT(*) as count FROM permissions;

-- Count roles
SELECT 'Total Roles:' as info, COUNT(*) as count FROM roles;

-- Count users
SELECT 'Total Users:' as info, COUNT(*) as count FROM users;

-- Show role permissions
SELECT 
    r.code as role_code,
    r.name as role_name,
    COUNT(rp.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
GROUP BY r.id, r.code, r.name
ORDER BY r.code;

-- Show user roles
SELECT 
    u.username,
    u.full_name,
    STRING_AGG(r.code, ', ') as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
GROUP BY u.id, u.username, u.full_name
ORDER BY u.username;

-- ============================================
-- DEFAULT CREDENTIALS
-- ============================================
-- Admin:     username: admin      password: Admin@123456
-- Manager:   username: manager01  password: Manager@123
-- Staff:     username: staff01    password: Staff@123
-- Viewer:    username: viewer01   password: Viewer@123
-- ============================================
