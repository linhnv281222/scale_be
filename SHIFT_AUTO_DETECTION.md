# Tính năng Tự động Gán Ca (Shift Auto-Detection)

## Tổng quan
Hệ thống tự động xác định và gán thông tin ca (shift) cho mỗi dữ liệu cân được thu thập, dựa trên thời gian thực tế của phép đo (`last_time`).

## Cơ chế hoạt động

### 1. **ShiftDetectionService**
Service chịu trách nhiệm phát hiện ca hiện tại dựa trên timestamp:

- **Vị trí**: [`org.facenet.service.shift.ShiftDetectionService`](src/main/java/org/facenet/service/shift/ShiftDetectionService.java)
- **Phương thức chính**: `detectCurrentShift(OffsetDateTime timestamp)`

**Thuật toán**:
1. Trích xuất `LocalTime` từ timestamp (chỉ lấy giờ:phút:giây)
2. Lấy tất cả các ca đang active (`isActive = true`)
3. So sánh thời gian với từng ca để tìm ca phù hợp
4. Xử lý đặc biệt cho ca qua đêm (ví dụ: 22:00 -> 06:00)

**Ví dụ**:
```java
// Ca 1: 08:00 - 16:00 (ca ngày)
// Ca 2: 16:00 - 00:00 (ca chiều)  
// Ca 3: 00:00 - 08:00 (ca đêm)

OffsetDateTime timestamp = OffsetDateTime.parse("2026-01-09T14:30:00+07:00");
Optional<Shift> shift = shiftDetectionService.detectCurrentShift(timestamp);
// Kết quả: Ca 1 (14:30 nằm trong khoảng 08:00-16:00)
```

### 2. **Tích hợp vào PersistenceServiceImpl**

Khi thu thập dữ liệu cân, hệ thống tự động:

**Bước 1**: Cập nhật `scale_current_states`
```java
// Trong updateCurrentState()
Optional<Shift> currentShift = shiftDetectionService.detectCurrentShift(lastTime.toOffsetDateTime());
currentShift.ifPresentOrElse(
    shift -> currentState.setShift(shift),
    () -> currentState.setShift(null)
);
```

**Bước 2**: Lưu vào `weighing_logs`
```java
// Trong insertWeighingLog()
WeighingLog log = WeighingLog.builder()
    .shift(currentState != null ? currentState.getShift() : null)
    // ... các trường khác
    .build();
```

### 3. **Database Schema**

**Bảng `weighing_logs`**:
```sql
ALTER TABLE weighing_logs 
ADD COLUMN shift_id BIGINT,
ADD CONSTRAINT fk_weighing_logs_shift FOREIGN KEY (shift_id) REFERENCES shifts(id);
```

**Bảng `scale_current_states`**:
```sql
ALTER TABLE scale_current_states 
ADD COLUMN shift_id BIGINT,
ADD CONSTRAINT fk_scale_current_states_shift FOREIGN KEY (shift_id) REFERENCES shifts(id);
```

## Cấu hình Ca

### Tạo ca mới qua API:
```http
POST /api/v1/shifts
Content-Type: application/json

{
  "code": "CA1",
  "name": "Ca sáng",
  "startTime": "08:00:00",
  "endTime": "16:00:00",
  "isActive": true
}
```

### Ví dụ cấu hình 3 ca:
```json
[
  {
    "code": "CA1",
    "name": "Ca sáng",
    "startTime": "08:00:00",
    "endTime": "16:00:00",
    "isActive": true
  },
  {
    "code": "CA2", 
    "name": "Ca chiều",
    "startTime": "16:00:00",
    "endTime": "00:00:00",
    "isActive": true
  },
  {
    "code": "CA3",
    "name": "Ca đêm", 
    "startTime": "00:00:00",
    "endTime": "08:00:00",
    "isActive": true
  }
]
```

## Xử lý Ca qua đêm

Service tự động xử lý ca qua đêm (overnight shift):

**Ví dụ**: Ca 3 từ 22:00 -> 06:00
- Thời gian **23:00**: Thuộc Ca 3 (>= 22:00)
- Thời gian **02:00**: Thuộc Ca 3 (< 06:00)
- Thời gian **14:00**: Không thuộc Ca 3

**Code logic**:
```java
private boolean isTimeInShift(LocalTime time, Shift shift) {
    LocalTime startTime = shift.getStartTime();
    LocalTime endTime = shift.getEndTime();
    
    if (startTime.isBefore(endTime)) {
        // Ca trong ngày: time >= start && time < end
        return !time.isBefore(startTime) && time.isBefore(endTime);
    } else {
        // Ca qua đêm: time >= start || time < end
        return !time.isBefore(startTime) || time.isBefore(endTime);
    }
}
```

## Lợi ích

### 1. **Tự động hóa hoàn toàn**
- Không cần set shift thủ công qua API
- Hệ thống tự động xác định dựa trên thời gian thực

### 2. **Phục vụ báo cáo theo ca**
```http
POST /api/v1/reports/export?importId=1
Content-Type: application/json

{
  "type": "WORD",
  "scaleIds": [1, 2, 3],
  "startTime": "2026-01-09T00:00:00+07:00",
  "endTime": "2026-01-09T23:59:59+07:00",
  "timeInterval": "SHIFT",
  "aggregationMethod": "SUM"
}
```

### 3. **Thống kê theo ca**
- Query dễ dàng: `WHERE shift_id = ?`
- Group by shift: `GROUP BY shift_id`
- Báo cáo chi tiết từng ca

### 4. **Kiểm toán và truy vết**
- Mỗi bản ghi weighing_log có thông tin ca
- Dễ dàng xác định dữ liệu thuộc ca nào
- Hỗ trợ phân tích hiệu suất theo ca

## Logging

Service ghi log chi tiết để debug:

```log
[SHIFT-DETECT] Time 14:30:00 matches shift CA1 (08:00:00->16:00:00)
[PERSISTENCE] Auto-detected shift CA1 for scale 1
```

Nếu không có ca phù hợp:
```log
[SHIFT-DETECT] Time 03:00:00 does not match any active shift
[PERSISTENCE] No shift detected for scale 1 at time 2026-01-09T03:00:00+07:00
```

## Migration

Chạy migration script để thêm cột shift_id:
```bash
# Migration sẽ tự động chạy khi khởi động application (Flyway)
# Hoặc chạy thủ công:
psql -U postgres -d scalehub -f db_migrations/V1_0_9__add_shift_id_to_weighing_tables.sql
```

## Kiểm tra dữ liệu

### Xem ca hiện tại của scale:
```sql
SELECT s.scale_id, s.shift_id, sh.code, sh.name, s.last_time
FROM scale_current_states s
LEFT JOIN shifts sh ON s.shift_id = sh.id;
```

### Thống kê dữ liệu theo ca:
```sql
SELECT sh.code, sh.name, COUNT(*) as total_records
FROM weighing_logs wl
LEFT JOIN shifts sh ON wl.shift_id = sh.id
WHERE wl.created_at >= '2026-01-09'
GROUP BY sh.code, sh.name;
```

## Lưu ý

1. **Ca phải được cấu hình trước**: Nếu không có ca nào active, shift_id sẽ là NULL
2. **Không chồng lấp**: Nên cấu hình ca không chồng lấp để tránh nhầm lẫn
3. **Thời gian chính xác**: Đảm bảo server time đồng bộ để xác định ca chính xác
4. **Múi giờ**: Hệ thống sử dụng OffsetDateTime để xử lý múi giờ chính xác
