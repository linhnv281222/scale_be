# Tính năng Manual Reading và Xử lý Shift Qua Đêm

## Tổng quan

Tài liệu này mô tả hai tính năng mới:
1. **Manual Reading**: Đọc dữ liệu chủ động tại đầu ca và cuối ca
2. **Overnight Shift Handling**: Xử lý ca qua đêm (ví dụ: 18:30 - 07:30 ngày hôm sau)

---

## 1. Manual Reading (Đọc dữ liệu chủ động)

### Mô tả
Bổ sung khả năng đọc dữ liệu chủ động ngoài việc auto reading định kỳ. Đặc biệt hữu ích cho:
- Đọc dữ liệu tại **đầu ca** (ghi nhận giá trị khởi đầu)
- Đọc dữ liệu tại **cuối ca** (ghi nhận giá trị kết thúc)
- Đọc dữ liệu **theo yêu cầu** (on-demand)

### API Endpoints

#### 1.1. Thực hiện Manual Reading

**POST** `/api/v1/scales/manual-reading`

**Request Body:**
```json
{
  "readingType": "SHIFT_START",  // hoặc "SHIFT_END", "ON_DEMAND"
  "scaleIds": [1, 2, 3],         // Optional: cân cụ thể, null = tất cả cân active
  "shiftId": 1,                   // Optional: ca cụ thể
  "note": "Đọc dữ liệu đầu ca"   // Optional: ghi chú
}
```

**Response:**
```json
{
  "status": "success",
  "data": {
    "triggered_at": "2026-01-15T08:00:00+07:00",
    "reading_type": "SHIFT_START",
    "shift_id": 1,
    "shift_code": "CA1",
    "total_scales": 10,
    "successful_readings": 9,
    "failed_readings": 1,
    "results": [
      {
        "scale_id": 1,
        "scale_name": "Cân 01",
        "success": true,
        "message": "Reading successful",
        "data_values": {
          "data1": "150.5",
          "data2": "25.3",
          "data3": "60.2",
          "data4": "1023.4",
          "data5": "50.0",
          "status": "ONLINE"
        }
      }
    ]
  }
}
```

#### 1.2. Đọc dữ liệu từ một cân cụ thể

**GET** `/api/v1/scales/manual-reading/{scaleId}`

Trả về dữ liệu hiện tại từ `scale_current_state` mà không lưu vào lịch sử.

**Response:**
```json
{
  "status": "success",
  "data": {
    "data1": "150.5",
    "data2": "25.3",
    "data3": "60.2",
    "data4": "1023.4",
    "data5": "50.0",
    "status": "ONLINE"
  }
}
```

### Reading Types

| Type | Mô tả | Sử dụng |
|------|-------|---------|
| `SHIFT_START` | Đọc tại đầu ca | Ghi nhận giá trị khởi đầu của ca |
| `SHIFT_END` | Đọc tại cuối ca | Ghi nhận giá trị kết thúc của ca |
| `ON_DEMAND` | Đọc theo yêu cầu | Đọc bất cứ lúc nào cần |

### Automatic Scheduling

Hệ thống tự động lên lịch đọc dữ liệu tại đầu ca và cuối ca cho tất cả các ca đang active.

**Khi nào schedule được tạo:**
- Khi ứng dụng khởi động (`ApplicationReadyEvent`)
- Khi tạo ca mới (nếu ca active)
- Khi cập nhật thời gian ca hoặc trạng thái active
- Khi xóa ca (soft delete)

**Ví dụ:**
```
Ca 1: 08:00 - 16:00
- Tự động đọc lúc 08:00 hàng ngày (SHIFT_START)
- Tự động đọc lúc 16:00 hàng ngày (SHIFT_END)

Ca 2: 16:00 - 00:00
- Tự động đọc lúc 16:00 hàng ngày (SHIFT_START)
- Tự động đọc lúc 00:00 hàng ngày (SHIFT_END)

Ca 3: 00:00 - 08:00
- Tự động đọc lúc 00:00 hàng ngày (SHIFT_START)
- Tự động đọc lúc 08:00 hàng ngày (SHIFT_END)
```

### Dữ liệu được lưu

Manual reading sẽ:
1. Đọc dữ liệu từ `scale_current_state` (dữ liệu mới nhất từ auto reading)
2. Lưu vào `weighing_logs` với:
   - `created_by`: `manual_shift_start`, `manual_shift_end`, hoặc `manual_on_demand`
   - `shift_id`: Ca tương ứng (nếu có)
   - `created_at` = `last_time` = thời điểm thực hiện manual reading

---

## 2. Overnight Shift Handling (Xử lý ca qua đêm)

### Mô tả
Hệ thống hỗ trợ đầy đủ các ca qua đêm (shift crosses midnight).

### Logic xử lý

**Ca trong ngày (Same-day shift):**
```
startTime < endTime
Ví dụ: 08:00 - 16:00

Kiểm tra: time >= startTime && time < endTime
- 10:00 -> IN
- 17:00 -> OUT
```

**Ca qua đêm (Overnight shift):**
```
startTime >= endTime
Ví dụ: 22:00 - 06:00

Kiểm tra: time >= startTime || time < endTime
- 23:00 -> IN (>= 22:00)
- 02:00 -> IN (< 06:00)
- 07:00 -> OUT
```

### Ví dụ cụ thể

#### Ca 18:30 - 07:30
```sql
INSERT INTO shifts (code, name, start_time, end_time, is_active)
VALUES ('CA3', 'Ca đêm', '18:30:00', '07:30:00', true);
```

**Phạm vi ca:**
- Từ 18:30 hôm nay đến 23:59:59 hôm nay -> IN
- Từ 00:00:00 ngày mai đến 07:30:00 ngày mai -> IN
- Các thời điểm khác -> OUT

**Timeline:**
```
17:00 -> OUT (trước 18:30)
19:00 -> IN  (sau 18:30)
23:00 -> IN  (sau 18:30, trước 00:00)
02:00 -> IN  (sau 00:00, trước 07:30)
06:00 -> IN  (sau 00:00, trước 07:30)
08:00 -> OUT (sau 07:30)
```

### Testing

```bash
# Test ca qua đêm
curl -X POST http://localhost:8080/api/v1/scales/manual-reading \
-H "Content-Type: application/json" \
-d '{
  "readingType": "SHIFT_START",
  "shiftId": 3
}'

# Test tại các thời điểm khác nhau trong ngày
# Hệ thống sẽ tự động detect shift đúng dựa trên thời gian hiện tại
```

---

## 3. Files Changed/Created

### New Files
1. `ManualReadingRequestDto.java` - DTO cho request manual reading
2. `ManualReadingResponseDto.java` - DTO cho response manual reading
3. `ManualReadingService.java` - Interface service
4. `ManualReadingServiceImpl.java` - Implementation service
5. `ManualReadingController.java` - REST API controller
6. `ShiftReadingSchedulerService.java` - Scheduler cho auto manual reading

### Modified Files
1. `ShiftDetectionService.java` - Cải thiện comments cho overnight shift logic
2. `ShiftServiceImpl.java` - Thêm refresh schedules khi shift thay đổi

---

## 4. Configuration

### Prerequisites
- Spring Boot TaskScheduler phải được configure (đã có sẵn trong project)
- Các ca (shifts) phải được tạo trong database với `is_active = true`

### Application Properties
```properties
# Không cần config thêm, sử dụng TaskScheduler mặc định
```

---

## 5. Use Cases

### Use Case 1: Đọc dữ liệu đầu ca thủ công
**Scenario:** Giám sát viên bắt đầu ca và muốn ghi nhận giá trị khởi đầu

```bash
POST /api/v1/scales/manual-reading
{
  "readingType": "SHIFT_START",
  "note": "Bắt đầu ca sáng"
}
```

### Use Case 2: Đọc dữ liệu cuối ca tự động
**Scenario:** Hệ thống tự động đọc dữ liệu khi kết thúc ca

- Được schedule tự động
- Chạy vào đúng thời điểm `endTime` của shift
- Lưu với `created_by = manual_shift_end`

### Use Case 3: Đọc dữ liệu theo yêu cầu
**Scenario:** Cần đọc dữ liệu ngay lập tức cho mục đích kiểm tra

```bash
POST /api/v1/scales/manual-reading
{
  "readingType": "ON_DEMAND",
  "scaleIds": [1, 2, 3],
  "note": "Kiểm tra định kỳ"
}
```

### Use Case 4: Ca qua đêm
**Scenario:** Ca làm việc từ 22:00 đến 06:00 sáng hôm sau

```sql
-- Tạo ca qua đêm
INSERT INTO shifts (code, name, start_time, end_time, is_active)
VALUES ('NIGHT', 'Ca đêm', '22:00:00', '06:00:00', true);
```

- Hệ thống tự động detect shift đúng cho bất kỳ thời điểm nào
- Auto reading sẽ gán đúng shift_id
- Manual reading tự động lên lịch lúc 22:00 và 06:00

---

## 6. Benefits

### Auto Reading (Existing)
- ✅ Đọc liên tục theo chu kỳ (mỗi vài giây)
- ✅ Theo dõi trạng thái real-time
- ✅ Phát hiện thay đổi nhanh chóng

### Manual Reading (New)
- ✅ Ghi nhận giá trị tại thời điểm quan trọng (đầu ca, cuối ca)
- ✅ Hỗ trợ tính toán data_1 = ABS(end - start) chính xác hơn
- ✅ Audit trail rõ ràng với created_by
- ✅ Linh hoạt đọc theo yêu cầu

### Overnight Shift Support
- ✅ Xử lý chính xác ca qua đêm
- ✅ Auto-detect shift đúng bất kể thời gian
- ✅ Schedule tự động cho cả ca qua đêm
- ✅ Không cần can thiệp thủ công

---

## 7. Monitoring & Logs

Tất cả operations đều được log với prefix:
- `[MANUAL-READING]` - Manual reading operations
- `[SHIFT-SCHEDULER]` - Shift scheduling operations
- `[SHIFT-DETECT]` - Shift detection operations

Ví dụ logs:
```
[MANUAL-READING] Starting manual reading: type=SHIFT_START, scaleIds=null, shiftId=1
[MANUAL-READING] Reading current data from scale 1
[MANUAL-READING] Saved manual reading for scale 1 with shift CA1 and type SHIFT_START
[MANUAL-READING] Completed: total=10, success=9, failed=1

[SHIFT-SCHEDULER] Initializing shift reading schedules
[SHIFT-SCHEDULER] Scheduled SHIFT_START reading for shift CA1 (Ca sáng) at 08:00
[SHIFT-SCHEDULER] Scheduled SHIFT_END reading for shift CA1 (Ca sáng) at 16:00

[SHIFT-DETECT] Time 02:00 matches shift NIGHT (22:00->06:00)
[SHIFT-DETECT] Overnight shift check: time=02:00, start=22:00, end=06:00, result=true
```

---

## 8. Database Impact

### weighing_logs
Thêm records với:
- `created_by` bắt đầu bằng `manual_*`
- Có thể query để phân biệt auto vs manual readings

```sql
-- Query manual readings
SELECT * FROM weighing_logs
WHERE created_by LIKE 'manual_%'
ORDER BY created_at DESC;

-- Query shift start readings
SELECT * FROM weighing_logs
WHERE created_by = 'manual_shift_start'
ORDER BY created_at DESC;

-- Count auto vs manual
SELECT 
  CASE 
    WHEN created_by LIKE 'manual_%' THEN 'Manual'
    ELSE 'Auto'
  END as reading_type,
  COUNT(*) as count
FROM weighing_logs
GROUP BY reading_type;
```
