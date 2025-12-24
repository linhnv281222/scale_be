-- Script kiểm tra hệ thống ScaleHub IoT
-- Chạy trong PostgreSQL để xem trạng thái cân và config

-- 1. Kiểm tra số lượng cân
SELECT 
    COUNT(*) as total_scales,
    COUNT(CASE WHEN is_active THEN 1 END) as active_scales,
    COUNT(CASE WHEN NOT is_active THEN 1 END) as inactive_scales
FROM scales;

-- 2. Liệt kê tất cả các cân với thông tin chi tiết
SELECT 
    s.id,
    s.name,
    s.model,
    s.is_active,
    l.name as location_name,
    CASE WHEN sc.scale_id IS NOT NULL THEN 'Yes' ELSE 'No' END as has_config
FROM scales s
LEFT JOIN locations l ON s.location_id = l.id
LEFT JOIN scale_configs sc ON s.id = sc.scale_id
ORDER BY s.id;

-- 3. Kiểm tra config của các cân ACTIVE
SELECT 
    sc.scale_id,
    s.name as scale_name,
    sc.protocol,
    sc.poll_interval,
    sc.conn_params->>'ip' as ip,
    sc.conn_params->>'port' as port,
    sc.conn_params->>'unit_id' as unit_id,
    sc.data_1->>'name' as data1_name,
    sc.data_1->>'is_used' as data1_used,
    sc.data_1->>'start_registers' as data1_start,
    sc.data_1->>'num_registers' as data1_count
FROM scale_configs sc
JOIN scales s ON sc.scale_id = s.id
WHERE s.is_active = true
ORDER BY sc.scale_id;

-- 4. Kiểm tra config bị thiếu hoặc không hợp lệ
SELECT 
    s.id,
    s.name,
    s.is_active,
    CASE 
        WHEN sc.scale_id IS NULL THEN 'No config'
        WHEN sc.conn_params IS NULL THEN 'Missing conn_params'
        WHEN sc.conn_params->>'ip' IS NULL AND sc.conn_params->>'com_port' IS NULL THEN 'Missing connection info'
        ELSE 'OK'
    END as config_status
FROM scales s
LEFT JOIN scale_configs sc ON s.id = sc.scale_id
WHERE s.is_active = true;

-- 5. Xem log measurement events gần đây (nếu có bảng này)
-- SELECT 
--     scale_id,
--     last_time,
--     data_1,
--     data_2,
--     status
-- FROM measurements
-- ORDER BY last_time DESC
-- LIMIT 20;
