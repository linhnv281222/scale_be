# Shift Result Management System - Lưu kết quả ca tự động

## Tổng quan

Hệ thống tự động lưu kết quả của mỗi ca làm việc, bao gồm giá trị bắt đầu, giá trị kết thúc và độ lệch của `data_1` cho mỗi cân.

---

## Kiến trúc hệ thống

```
ShiftReadingSchedulerService (Schedule theo thời gian ca)
    ↓
ManualReadingService (Đọc dữ liệu từ các cân)
    ↓
ShiftResultService (Tạo/Update shift result)
    ↓
shift_results table (Lưu kết quả vào database)
```

---

## 1. Database Schema

### Bảng `shift_results`

```sql
CREATE TABLE shift_results (
    id BIGSERIAL PRIMARY KEY,
    scale_id BIGINT NOT NULL,                -- ID cân
    shift_id BIGINT NOT NULL,                -- ID ca
    shift_date DATE NOT NULL,                -- Ngày của ca
    start_value_data1 VARCHAR(50),          -- Giá trị đầu ca
    end_value_data1 VARCHAR(50),            -- Giá trị cuối ca
    deviation_data1 DECIMAL(15, 2),         -- Độ lệch (end - start)
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    CONSTRAINT uk_shift_result_scale_shift_date UNIQUE (scale_id, shift_id, shift_date)
);
```

**Unique Constraint**: Mỗi cân chỉ có một kết quả cho mỗi ca trong một ngày.

---

## 2. Luồng hoạt động tự động

### 2.1. Khởi động hệ thống

```
ApplicationReadyEvent
    ↓
ShiftReadingSchedulerService.initializeShiftSchedules()
    ↓
Tạo scheduled tasks cho tất cả shifts active
```

### 2.2. Lên lịch cho mỗi ca

Với mỗi shift active, hệ thống tạo 2 scheduled tasks:

1. **SHIFT_START Task**: Chạy vào thời gian `shift.start_time`
2. **SHIFT_END Task**: Chạy vào thời gian `shift.end_time`

**Ví dụ:**
```
Shift CA1: 08:00 - 16:00
  → Schedule SHIFT_START at 08:00 (cron: "0 0 8 * * *")
  → Schedule SHIFT_END at 16:00 (cron: "0 0 16 * * *")

Shift CA2: 22:00 - 06:00 (overnight)
  → Schedule SHIFT_START at 22:00 (cron: "0 0 22 * * *")
  → Schedule SHIFT_END at 06:00 (cron: "0 0 6 * * *")
```

### 2.3. Khi đến thời điểm bắt đầu ca

```
Cron trigger at shift start time
    ↓
ShiftReadingSchedulerService.performShiftReading()
    ↓
ManualReadingService.performManualReading(type=SHIFT_START, shiftId=X)
    ↓
Đọc data_1 từ scale_current_state
    ↓
Lưu vào weighing_logs (created_by = "manual_shift_start")
    ↓
ShiftResultService.createOrUpdateStartValue()
    ↓
Tạo shift_result mới với start_value_data1
```

### 2.4. Khi đến thời điểm kết thúc ca

```
Cron trigger at shift end time
    ↓
ShiftReadingSchedulerService.performShiftReading()
    ↓
ManualReadingService.performManualReading(type=SHIFT_END, shiftId=X)
    ↓
Đọc data_1 từ scale_current_state
    ↓
Lưu vào weighing_logs (created_by = "manual_shift_end")
    ↓
ShiftResultService.updateEndValue()
    ↓
Update shift_result với end_value_data1
    ↓
Tính deviation_data1 = end_value - start_value
```

---

## 3. Xử lý ca qua đêm

### Vấn đề

Với ca qua đêm (ví dụ: 22:00 → 06:00), cần xác định đúng ngày của ca:
- Lúc 22:00 ngày 15/01 → shift_date = 15/01 ✅
- Lúc 06:00 ngày 16/01 → shift_date = 15/01 ✅ (không phải 16/01)

### Giải pháp

Method `determineShiftDate()` trong ManualReadingServiceImpl:

```java
private LocalDate determineShiftDate(Shift shift, ReadingType readingType) {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    
    boolean isOvernightShift = shift.getEndTime().isBefore(shift.getStartTime());
    
    if (!isOvernightShift) {
        return today; // Ca trong ngày: luôn dùng ngày hiện tại
    }
    
    // Ca qua đêm
    if (readingType == SHIFT_START) {
        return today; // Bắt đầu ca: luôn dùng ngày hiện tại
    } else {
        // Kết thúc ca qua đêm
        if (now.isBefore(shift.getStartTime())) {
            return today.minusDays(1); // Đang ở phần sau nửa đêm → ca bắt đầu hôm qua
        } else {
            return today; // Đang ở phần trước nửa đêm → ca bắt đầu hôm nay
        }
    }
}
```

**Ví dụ cụ thể:**

Ca 22:00 → 06:00:
- Lúc 22:00 ngày 15/01 (SHIFT_START):
  - `now = 22:00`, `isOvernightShift = true`, `readingType = SHIFT_START`
  - → `shiftDate = 15/01`
  
- Lúc 06:00 ngày 16/01 (SHIFT_END):
  - `now = 06:00`, `isOvernightShift = true`, `readingType = SHIFT_END`
  - `now (06:00) < shift.startTime (22:00)` → true
  - → `shiftDate = 15/01` (lùi về ngày hôm trước)

---

## 4. API Endpoints

### 4.1. Lấy thông tin shift result

#### GET `/api/v1/shift-results/{id}`
Lấy shift result theo ID.

**Response:**
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "scale_id": 1,
    "scale_name": "Cân 01",
    "shift_id": 1,
    "shift_code": "CA1",
    "shift_name": "Ca sáng",
    "shift_date": "2026-01-15",
    "start_value_data1": "1500.50",
    "end_value_data1": "2500.75",
    "deviation_data1": 1000.25,
    "is_active": true,
    "created_at": "2026-01-15T08:00:00+07:00",
    "created_by": "system",
    "updated_at": "2026-01-15T16:00:00+07:00",
    "updated_by": "system"
  }
}
```

#### GET `/api/v1/shift-results/scale/{scaleId}`
Lấy tất cả shift results của một cân.

#### GET `/api/v1/shift-results/scale/{scaleId}/shift/{shiftId}/date/{date}`
Lấy shift result cụ thể theo cân, ca và ngày.

**Example:**
```bash
GET /api/v1/shift-results/scale/1/shift/1/date/2026-01-15
```

#### GET `/api/v1/shift-results?fromDate=&toDate=&scaleId=`
Lấy shift results theo khoảng thời gian.

**Query Parameters:**
- `fromDate` (required): Ngày bắt đầu (YYYY-MM-DD)
- `toDate` (required): Ngày kết thúc (YYYY-MM-DD)
- `scaleId` (optional): Lọc theo cân cụ thể

**Example:**
```bash
GET /api/v1/shift-results?fromDate=2026-01-01&toDate=2026-01-31
GET /api/v1/shift-results?fromDate=2026-01-01&toDate=2026-01-31&scaleId=1
```

### 4.2. Cập nhật shift result

#### PUT `/api/v1/shift-results/{id}`
Cập nhật shift result thủ công (ví dụ: sửa giá trị sai).

**Request Body:**
```json
{
  "start_value_data1": "1505.00",
  "end_value_data1": "2505.00",
  "deviation_data1": 1000.00,
  "is_active": true
}
```

**Lưu ý:** 
- Nếu cung cấp cả `start_value_data1` và `end_value_data1`, hệ thống sẽ tự động tính lại `deviation_data1`
- Có thể cung cấp `deviation_data1` trực tiếp nếu không muốn tự động tính

### 4.3. Xóa shift result

#### DELETE `/api/v1/shift-results/{id}`
Xóa shift result (soft delete).

---

## 5. Quản lý schedules

### 5.1. Tự động refresh khi thay đổi shift

Khi tạo, cập nhật hoặc xóa shift, hệ thống tự động refresh schedules:

```java
ShiftServiceImpl:
  create() → refreshShiftSchedules()
  update() → refreshShiftSchedules()
  delete() → refreshShiftSchedules()
```

### 5.2. Thủ công refresh schedules

```java
shiftReadingSchedulerService.refreshSchedules();
```

---

## 6. Monitoring & Logs

### Log patterns

**Khi schedule được tạo:**
```
[SHIFT-SCHEDULER] Scheduled SHIFT_START reading for shift CA1 (Ca sáng) at 08:00
[SHIFT-SCHEDULER] Scheduled SHIFT_END reading for shift CA1 (Ca sáng) at 16:00
```

**Khi thực hiện reading:**
```
[SHIFT-SCHEDULER] Executing scheduled SHIFT_START reading for shift CA1
[MANUAL-READING] Starting manual reading: type=SHIFT_START, scaleIds=null, shiftId=1
[MANUAL-READING] Updated shift result start value for scale 1 shift CA1 on 2026-01-15
```

**Khi hoàn thành:**
```
[SHIFT-SCHEDULER] Completed SHIFT_START reading for shift CA1: success=10, failed=0
```

### Query để kiểm tra

```sql
-- Xem tất cả shift results hôm nay
SELECT sr.*, s.name as scale_name, sh.code as shift_code
FROM shift_results sr
JOIN scales s ON sr.scale_id = s.id
JOIN shifts sh ON sr.shift_id = sh.id
WHERE sr.shift_date = CURRENT_DATE
ORDER BY sr.scale_id, sh.start_time;

-- Xem shift results chưa có end value
SELECT sr.*, s.name as scale_name, sh.code as shift_code
FROM shift_results sr
JOIN scales s ON sr.scale_id = s.id
JOIN shifts sh ON sr.shift_id = sh.id
WHERE sr.end_value_data1 IS NULL
ORDER BY sr.shift_date DESC, sr.scale_id;

-- Xem shift results với deviation lớn
SELECT sr.*, s.name as scale_name, sh.code as shift_code
FROM shift_results sr
JOIN scales s ON sr.scale_id = s.id
JOIN shifts sh ON sr.shift_id = sh.id
WHERE ABS(sr.deviation_data1) > 1000
ORDER BY ABS(sr.deviation_data1) DESC;
```

---

## 7. Quy trình troubleshooting

### Vấn đề: Shift result không được tạo

**Kiểm tra:**
1. Shift có active không?
   ```sql
   SELECT * FROM shifts WHERE is_active = true;
   ```

2. Schedules đã được tạo chưa?
   - Xem logs: `[SHIFT-SCHEDULER] Scheduled`
   - Restart application để trigger `ApplicationReadyEvent`

3. Cron expression có đúng không?
   - Check logs khi tạo schedule
   - Verify timezone: `ZoneId.systemDefault()`

### Vấn đề: Shift date sai cho ca qua đêm

**Kiểm tra:**
1. Shift có phải ca qua đêm không?
   ```sql
   SELECT *, (end_time < start_time) as is_overnight 
   FROM shifts WHERE id = X;
   ```

2. Thời gian trigger có đúng không?
   - SHIFT_END cho ca qua đêm phải chạy sau 00:00
   - Logic `determineShiftDate()` sẽ lùi về ngày hôm trước

### Vấn đề: Deviation không được tính

**Kiểm tra:**
1. Cả start và end values đều có giá trị?
   ```sql
   SELECT * FROM shift_results 
   WHERE start_value_data1 IS NOT NULL 
   AND end_value_data1 IS NOT NULL 
   AND deviation_data1 IS NULL;
   ```

2. Giá trị có parse được thành số không?
   - Check logs: `Failed to calculate deviation`

---

## 8. Best Practices

1. **Backup trước khi update thủ công**
   ```sql
   -- Backup shift result trước khi update
   INSERT INTO shift_results_backup 
   SELECT * FROM shift_results WHERE id = X;
   ```

2. **Monitor failed readings**
   - Set up alerts cho `[SHIFT-SCHEDULER] Error`
   - Định kỳ check shift results với NULL values

3. **Định kỳ kiểm tra schedules**
   - Sau mỗi lần thay đổi shifts
   - Sau restart application
   - Verify trong logs: `Scheduled readings for X shifts`

4. **Archive dữ liệu cũ**
   ```sql
   -- Archive shift results cũ hơn 6 tháng
   INSERT INTO shift_results_archive
   SELECT * FROM shift_results 
   WHERE shift_date < CURRENT_DATE - INTERVAL '6 months';
   ```

---

## 9. Performance Considerations

- **Indexes**: Đã tạo indexes cho `scale_id`, `shift_id`, `shift_date`
- **Unique constraint**: Đảm bảo không duplicate records
- **Async processing**: Scheduled tasks chạy độc lập, không block main thread
- **Error handling**: Lỗi trong shift result không làm fail manual reading

---

## 10. Future Enhancements

- [ ] Thêm notification khi shift result được tạo
- [ ] Export shift results to Excel/PDF
- [ ] Dashboard hiển thị shift results realtime
- [ ] Alert khi deviation vượt ngưỡng
- [ ] History tracking cho manual updates
- [ ] Batch update cho nhiều scales
