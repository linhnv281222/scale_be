# Module 2: Quản lý Cấu hình (Full CRUD Specification)

## 1. Thực thể: Vị trí (Locations)

### 1.1. Create Location

* **POST** `/api/v1/locations`
* **Payload:** `{"code": "WS_01", "name": "Xưởng A", "parent_id": null}`
* **Response (201):**
```json
{
  "id": 1, "code": "WS_01", "name": "Xưởng A", "parent_id": null,
  "created_at": "2025-12-23T11:00:00Z", "created_by": "admin_user",
  "updated_at": "2025-12-23T11:00:00Z", "updated_by": "admin_user"
}

```



### 1.2. Read Location (All / Tree / Detail)

* **GET** `/api/v1/locations` (Danh sách phẳng)
* **GET** `/api/v1/locations/tree` (Danh sách phân cấp)
* **GET** `/api/v1/locations/{id}` (Chi tiết 1 vị trí)

### 1.3. Update Location

* **PUT** `/api/v1/locations/{id}`
* **Payload:** `{"name": "Xưởng A - Khu đóng gói", "parent_id": null}`
* **Response (200):** Trả về object sau cập nhật (kèm Audit fields mới).

### 1.4. Delete Location

* **DELETE** `/api/v1/locations/{id}`
* **Ràng buộc:** Trả về lỗi 400 nếu vị trí còn `Scales` hoặc `Sub-locations` bên trong.

---

## 2. Thực thể: Thiết bị Cân (Scales)

### 2.1. Create Scale

* **POST** `/api/v1/scales`
* **Payload:** `{"name": "Cân 01", "location_id": 1, "model": "IND570", "is_active": true}`
* **Response (201):**
```json
{
  "id": 100, "name": "Cân 01", "location_id": 1, "model": "IND570", "is_active": true,
  "created_at": "2025-12-23T11:05:00Z", "created_by": "system",
  "updated_at": "2025-12-23T11:05:00Z", "updated_by": "system"
}

```



### 2.2. Read Scale (List / Detail)

* **GET** `/api/v1/scales?location_id=1` (Lọc theo vị trí)
* **GET** `/api/v1/scales/{id}` (Chi tiết cân)

### 2.3. Update Scale (Thông tin cơ bản)

* **PUT** `/api/v1/scales/{id}`
* **Payload:** `{"name": "Cân 01 - Đã hiệu chuẩn", "location_id": 1, "is_active": false}`
* **Response (200):** Object Scale sau cập nhật.

### 2.4. Delete Scale

* **DELETE** `/api/v1/scales/{id}`
* **Lưu ý:** Xóa Cascade dữ liệu trong `scale_configs` và `scale_current_states`.

---

## 3. Thực thể: Cấu hình kỹ thuật (Scale Configs)

### 3.1. Create Config (Khởi tạo)

* **Thực hiện tự động:** Khi tạo Scale thành công ở mục 2.1, Backend tự động Insert bản ghi mặc định vào bảng này.

### 3.2. Read Config (Chi tiết cấu hình)

* **GET** `/api/v1/scales/{id}/config`
* **Response (200):**
```json
{
  "scale_id": 100,
  "protocol": "MODBUS_TCP",
  "poll_interval": 1000,
  "conn_params": { "ip": "192.168.1.10", "port": 502 },
  "data_1": { "name": "Weight", "start_registers": 40001, "num_registers": 2, "is_used": true },
  "data_2": { "is_used": false }, "data_3": { "is_used": false },
  "data_4": { "is_used": false }, "data_5": { "is_used": false },
  "updated_at": "2025-12-23T11:10:00Z", "updated_by": "tech_lead"
}

```



### 3.3. Update Config (Cập nhật kỹ thuật)

* **PUT** `/api/v1/scales/{id}/config`
* **Payload:**
```json
{
  "protocol": "MODBUS_TCP",
  "poll_interval": 500,
  "conn_params": { "ip": "192.168.1.15", "port": 502 },
  "data_1": { "name": "Net Weight", "start_registers": 0, "num_registers": 2, "is_used": true },
  "data_2": { "name": "Status", "start_registers": 2, "num_registers": 1, "is_used": true },
  "data_3": { "is_used": false }, "data_4": { "is_used": false }, "data_5": { "is_used": false }
}

```



### 3.4. Delete Config

* **Thao tác:** Thường không có API xóa riêng cho Config vì nó đi liền với vòng đời của `Scale`. Xóa Scale sẽ tự xóa Config.

---

## 4. Tổng kết quy tắc Audit & Validation

| Quy tắc | Chi tiết |
| --- | --- |
| **Audit Field** | `created_by` và `updated_by` phải trả về **Username** (ví dụ: "hoang_iot"), không trả về ID. |
| **JSONB Schema** | Bắt buộc 5 trường `data_1..5` phải tuân thủ đúng object `{name, start_registers, num_registers, is_used}`. |
| **Runtime Trigger** | Sau khi **PUT Config** thành công, hệ thống phải gửi tín hiệu **Hot-reload** cho Engine Worker. |
