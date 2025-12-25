# Module 5: Báo cáo & Thống kê (Cầm tay chỉ việc)

## Phần 1: Tinh chỉnh tầng lưu trữ (Persistence Refinement)

Trước đây, chúng ta có thể định nghĩa lưu JSON Object. Nhưng theo yêu cầu mới, bạn sẽ lưu **Plain String** vào cột JSONB.

**1. Cách đẩy dữ liệu vào DB:**
Khi Engine trả về chuỗi `"150.5"`, lệnh Insert/Update vào Postgres sẽ không bọc thêm ngoặc nhọn `{}`.
Tức là các trường data_n trong current_state và log không còn là cấu trúc JSON nữa
* **Lưu ý kỹ thuật:** Trong Postgres, cột JSONB vẫn chấp nhận một chuỗi đơn lẻ (gọi là JSON scalar).
* **Thực hiện:** Khi lưu, bạn chỉ cần gán thẳng giá trị String đó cho tham số của câu lệnh SQL.

**2. Trạng thái trong DB sẽ trông như thế này:**

* Cột `data_1`: `"150.5"` (có dấu ngoặc kép của chuẩn JSONB bao quanh chuỗi).
* Cột `data_2`: `"1"` (biểu thị trạng thái).

---

## Phần 2: Xây dựng Module Báo cáo & Thống kê

Hệ thống báo cáo này sẽ hoạt động như một máy lọc dữ liệu khổng lồ.

### 1. Đầu vào của báo cáo (Report Request DTO)

Để làm báo cáo, Backend cần nhận được 5 thông tin sau từ UI:

1. **List IDs cân:** Ví dụ `[1, 2, 10]` (Người dùng tích chọn trên màn hình).
2. **Trường dữ liệu:** `data_1`, `data_2`,... (Người dùng chọn "Thống kê Khối lượng" hay "Thống kê nhiệt độ").
3. **Phép toán (Method):** `SUM`, `AVG`, hoặc `MAX`.
4. **Khoảng thời gian (Range):** `From Date` - `To Date`.
5. **Cấp độ nhóm (Interval):** `Giờ`, `Ngày`, `Tuần`, `Tháng`, `Năm`.

---

### 2. Logic xử lý "Ép kiểu" (The Casting Magic)

Vì dữ liệu là **String**, bạn không thể `SUM` trực tiếp. Bạn phải "dạy" cho Database cách chuyển chuỗi đó thành số.

**Quy trình xử lý trong câu lệnh SQL:**

1. **Trích xuất:** Dùng toán tử `(data_1 ->> 0)` để lấy giá trị chuỗi ra khỏi định dạng JSONB.
2. **Làm sạch:** Dùng hàm `NULLIF` hoặc `Regex` để loại bỏ các trường hợp chuỗi không phải là số (ví dụ cân trả về "ERROR" hoặc chuỗi rỗng ""). Nếu là lỗi, ta coi nó là `0`.
3. **Ép kiểu:** Chuyển sang kiểu số thực: `::NUMERIC`.

**Ví dụ một đoạn công thức SQL mẫu:**
`SUM(CASE WHEN (data_1 ->> 0) ~ '^[0-9\.]+$' THEN (data_1 ->> 0)::NUMERIC ELSE 0 END)`
*(Dịch: Nếu data_1 là định dạng số thì cộng vào, nếu không thì cộng 0).*

---

### 3. Chia nhóm theo thời gian (Time Bucketing)

Đây là phần "cầm tay chỉ việc" quan trọng để báo cáo trông đẹp mắt. Tùy vào lựa chọn của user, bạn sẽ dùng hàm `date_trunc` của Postgres:

* **Theo Giờ:** Nhóm tất cả các bản ghi có chung `năm-tháng-ngày giờ`.
* **Theo Ngày:** Nhóm theo `năm-tháng-ngày`.
* **Theo Tuần:** Postgres sẽ tự tìm ngày đầu tuần (Thứ 2) để nhóm vào.
* **Theo Tháng:** Nhóm theo `năm-tháng`.

---

### 4. Triển khai 2 luồng báo cáo riêng biệt

#### Luồng A: Báo cáo nhanh (Ad-hoc) - Dùng cho Giờ/Ngày

Khi user chọn xem báo cáo trong 24h qua:

1. Hệ thống quét trực tiếp vào bảng `weighing_logs`.
2. Dùng các hàm ép kiểu và nhóm thời gian ở trên để tính toán.
3. **Ưu điểm:** Dữ liệu mới nhất (vừa cân xong 1 giây trước) sẽ hiện lên báo cáo ngay.

#### Luồng B: Báo cáo tổng hợp (Pre-aggregated) - Dùng cho Tuần/Tháng/Năm

Vì bảng Log có thể có hàng tỷ dòng, bạn không thể quét từng dòng để tính tổng cả năm được.

1. **Daily Job:** Mỗi đêm lúc 00:01, hệ thống chạy một tiến trình ngầm quét bảng `weighing_logs` của ngày hôm trước.
2. Nó tính sẵn `SUM`, `AVG`, `MAX` cho từng cân và lưu vào bảng `scale_daily_reports`.
3. Khi user xem báo cáo Tháng/Năm, bạn chỉ cần đọc từ bảng `scale_daily_reports`.
4. **Kết quả:** Báo cáo cả năm hiện ra trong 0.5 giây thay vì treo máy.

---

### 5. Kết quả trả về cho Frontend (The Payload)

Bạn phải trả về một mảng để Frontend vẽ biểu đồ (Chart.js hoặc Highcharts).

**Mẫu dữ liệu trả về:**

```json
{
  "report_name": "Tổng khối lượng theo ngày",
  "method": "SUM",
  "data_points": [
    { "time": "2025-12-20", "value": 5500.25 },
    { "time": "2025-12-21", "value": 4800.10 },
    { "time": "2025-12-22", "value": 6100.00 }
  ]
}

```

---

### Tóm tắt các bước bạn cần code ngay:

1. **Viết 1 hàm SQL Utility:** Để trích xuất và ép kiểu JSONB String sang Numeric.
2. **Viết API Service:** Nhận các tham số filter (IDs, Method, Interval, Range).
3. **Xây dựng Query Dynamic:** Tùy vào `Interval` user chọn (Giờ/Ngày...) mà cộng chuỗi câu lệnh `GROUP BY` tương ứng.