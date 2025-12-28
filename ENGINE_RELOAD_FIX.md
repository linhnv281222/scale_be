# Fix: Engine không tuân theo Poll Interval từ DB

## 🔍 Vấn đề

Các engine (ModbusTcpEngine, ModbusRtuEngine) **không sử dụng poll interval mới** sau khi update config trong database. Engine vẫn dùng poll interval cũ cho đến khi restart ứng dụng.

## 🐛 Nguyên nhân

**JPA Persistence Context Cache** giữ entity cũ trong memory:

1. Khi `updateScaleConfig()` được gọi, config mới được save vào DB ✅
2. Event `ConfigChangedEvent` được publish ✅
3. `EngineManager.restartEngine()` được gọi ✅
4. Engine cũ bị stop ✅
5. `scaleRepository.findByIdWithDetails()` được gọi ❌
   - **Vấn đề**: JPA EntityManager trả về **cached entity** từ persistence context
   - Entity này có `pollInterval` CŨ, chưa được refresh từ DB
6. Engine mới được tạo với config CŨ ❌

## ✅ Giải pháp

### File: `EngineManager.java`

**Thêm `entityManager.clear()`** trong method `restartEngine()`:

```java
@Transactional
public void restartEngine(Long scaleId) {
    log.info("[EngineManager] Restarting engine for scale {}", scaleId);
    
    // Dừng engine cũ
    stopEngine(scaleId);
    
    // CRITICAL FIX: Clear persistence context để tránh lấy cached entity cũ
    entityManager.clear();
    log.debug("[EngineManager] Cleared EntityManager persistence context");
    
    // Load lại config MỚI từ DB
    scaleRepository.findByIdWithDetails(scaleId).ifPresent(scale -> {
        ScaleConfig config = scale.getConfig();
        log.info("[EngineManager] Loaded fresh config - pollInterval={}ms",
                config.getPollInterval());
        
        if (scale.getIsActive()) {
            startEngine(scale);
        }
    });
}
```

### File: `ModbusTcpEngine.java` & `ModbusRtuEngine.java`

**Thêm log để verify** poll interval:

```java
@Override
public void run() {
    running = true;
    log.info("[Engine {}] Started with pollInterval={}ms", 
            config.getScaleId(), config.getPollInterval());
    
    // ... trong vòng lặp ...
    
    int pollInterval = config.getPollInterval();
    log.trace("[Engine {}] Sleeping for {}ms", config.getScaleId(), pollInterval);
    Thread.sleep(pollInterval);
}
```

## 🧪 Cách test

### 1. Kiểm tra poll interval hiện tại

```sql
-- Xem config hiện tại của scale
SELECT scale_id, protocol, poll_interval 
FROM scale_configs 
WHERE scale_id = 1;
```

### 2. Update poll interval qua API

```bash
# Update poll_interval từ 1000ms -> 3000ms
curl -X PUT http://localhost:8080/api/scales/1/config \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "protocol": "MODBUS_TCP",
    "poll_interval": 3000,
    "conn_params": {
      "ip": "192.168.1.10",
      "port": 502,
      "unit_id": 1
    },
    "data_1": {
      "used": true,
      "start_registers": 0,
      "num_registers": 2
    }
  }'
```

### 3. Kiểm tra log

Sau khi gọi API update, bạn sẽ thấy log:

```log
[EngineManager] Restarting engine for scale 1
[EngineManager] Cleared EntityManager persistence context
[EngineManager] Loaded fresh config - pollInterval=3000ms
[Engine 1] Modbus TCP Engine started with pollInterval=3000ms
[Engine 1] Sleeping for 3000ms (pollInterval)
```

### 4. Verify trong DB

```sql
-- Confirm DB đã update
SELECT scale_id, poll_interval, updated_at 
FROM scale_configs 
WHERE scale_id = 1;
```

## 📊 Kết quả

| Trước fix | Sau fix |
|-----------|---------|
| ❌ Engine dùng poll interval cũ | ✅ Engine dùng poll interval mới ngay lập tức |
| ❌ Phải restart app để apply config mới | ✅ Hot-reload, không cần restart app |
| ❌ JPA cache entity cũ | ✅ Force reload từ DB |

## 🔧 Technical Details

### EntityManager.clear()

```java
entityManager.clear(); // Detach ALL entities từ persistence context
```

- **Tác dụng**: Xóa toàn bộ managed entities trong persistence context
- **Kết quả**: Query tiếp theo sẽ hit database thay vì cache
- **Trade-off**: Các entity khác cũng bị detach (acceptable vì scope nhỏ)

### Alternative approaches (không dùng)

1. ❌ `entityManager.refresh(entity)` - Requires entity trong persistence context
2. ❌ `@CacheEvict` - Chỉ evict Spring Cache, không ảnh hưởng JPA cache
3. ❌ Query hint `FLUSH_MODE` - Không giải quyết vấn đề cache read

## 📝 Notes

- Fix này áp dụng cho **mọi loại config update** (conn_params, data slots, protocol, ...)
- Log level `TRACE` được thêm để debug chi tiết (có thể tắt nếu quá nhiều log)
- `@Transactional` cần thiết để sử dụng EntityManager

## ✅ Status

- [x] Identified root cause
- [x] Implemented fix in EngineManager
- [x] Added comprehensive logging
- [x] Tested with poll_interval updates
- [x] Documented solution

---
**Author**: GitHub Copilot  
**Date**: 2025-12-29  
**Issue**: Engine không tuân theo poll interval được config trong DB
