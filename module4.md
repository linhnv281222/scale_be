Để triển khai **Module 4: Persistence (Lưu trữ dữ liệu)** cho hệ thống 300 cân công nghiệp, bạn cần tập trung vào việc chuyển hóa các "sự kiện" từ hàng đợi (In-memory Queue) thành các bản ghi vật lý trong Database một cách bền vững.

Dưới đây là mô tả chi tiết quy trình triển khai mà không dùng mã giả:

---

## 1. Cơ chế kích hoạt lưu trữ (Triggering)

Việc lưu trữ không diễn ra tại Engine mà diễn ra tại **Core Processing**. Khi một Worker lấy được một "Bao thư" dữ liệu (MeasurementEvent) ra khỏi Queue, nó sẽ thực hiện đồng thời hai việc: đẩy dữ liệu lên giao diện (WebSocket) và gọi xuống tầng Persistence để ghi vào đĩa.

---

## 2. Quy trình xử lý dữ liệu tại tầng Persistence

Khi dữ liệu được chuyển xuống, hệ thống thực hiện các bước xử lý logic sau:

* **Chuyển đổi kiểu dữ liệu (Data Mapping):** Vì dữ liệu từ Engine gửi đi đang ở dạng **String** (ví dụ: `"150.5"`), tầng Persistence sẽ đóng gói chuỗi này vào một đối tượng JSON (ví dụ: `{"val": "150.5"}`) để khớp với các cột `data_1` đến `data_5` đang để kiểu **JSONB** trong DB.
* **Xác định thời gian (Time Handling):** Hệ thống sẽ lấy trường `last_time` từ sự kiện (thời điểm cân trả số) để lưu vào cột `last_time`. Đồng thời, Database sẽ tự sinh `created_at` (thời điểm insert) để đối chiếu độ trễ nếu cần.
* **Xử lý Audit:** Vì đây là luồng tự động từ máy móc, các trường `created_by` và `updated_by` sẽ được hệ thống tự động điền giá trị là `"system"` hoặc `"engine_modbus"`.

---

## 3. Chiến lược ghi vào hai bảng trọng tâm

Dữ liệu sau khi xử lý sẽ được phân phối vào hai đích đến khác nhau:

### 3.1. Cập nhật trạng thái tức thời (`scale_current_states`)

* **Hình thức:** Đây là thao tác **Ghi đè (Upsert)**. Mỗi trạm cân chỉ có duy nhất một dòng trong bảng này.
* **Mục đích:** Phục vụ màn hình Dashboard tổng quát. Khi user nhìn vào sẽ biết ngay cân số 5 đang ở trạng thái Online và khối lượng hiện tại là bao nhiêu.
* **Lưu ý:** Thao tác này diễn ra liên tục (ví dụ 1 giây/lần), giúp bảng luôn phản ánh "hơi thở" thực tế của thiết bị.

### 3.2. Ghi lịch sử chi tiết (`weighing_logs`)

* **Hình thức:** Đây là thao tác **Chèn mới (Insert)**. Mọi dữ liệu đọc được đều tạo ra một dòng mới.
* **Mục đích:** Phục vụ báo cáo, vẽ biểu đồ xu hướng và truy vết lỗi trong quá khứ.
* **Lưu ý kỹ thuật:** Vì bảng này sẽ phình to rất nhanh (khoảng 25-30 triệu bản ghi mỗi ngày với 300 cân), việc triển khai **Partitioning** (phân vùng dữ liệu) theo ngày hoặc theo tháng ngay từ đầu là bắt buộc để đảm bảo tốc độ truy vấn sau này.

---

## 4. Tối ưu hóa hiệu năng (Performance Strategy)

Với tần suất ~300 bản ghi/giây, việc ghi đơn lẻ từng dòng có thể làm quá tải ổ đĩa (Disk I/O). Cách triển khai thực tế sẽ là:

* **Sử dụng Pool kết nối (Connection Pooling):** Duy trì một lượng kết nối sẵn có tới PostgreSQL để không mất công khởi tạo lại cho mỗi bản ghi.
* **Cơ chế Ghi gom (Batch Ingestion):** Thay vì cứ có 1 tin nhắn là gọi DB ngay, hệ thống sẽ gom khoảng 50-100 tin nhắn hoặc chờ sau 500ms rồi mới thực hiện một lệnh "Insert All" duy nhất. Điều này giảm tải cho Database tới 80-90%.
* **Xử lý bất đồng bộ (Asynchronous Writing):** Việc ghi DB sẽ được chạy trên các luồng (Thread) riêng biệt để nếu Database có chậm, nó cũng không làm nghẽn luồng đọc dữ liệu từ cân.

---

## 5. Kiểm soát lỗi (Error Handling)

Hệ thống cần có cơ chế "ghi nhớ" nếu việc lưu trữ thất bại:

* Nếu Database mất kết nối, hệ thống sẽ Log lỗi chi tiết và có thể lưu tạm vào một file log dự phòng (Dead Letter File).
* Các sự kiện lỗi được lưu trữ để sau khi Database phục hồi, kỹ thuật viên có thể biết được khoảng thời gian nào dữ liệu bị gián đoạn (Data Gap).
