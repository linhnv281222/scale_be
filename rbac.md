# API Specification: Module 1 – RBAC Management

**Base URL:** `/api/v1`

**Content-Type:** `application/json`

---

## 1. Quản lý Permissions (Quyền)

### 1.1 Lấy danh sách quyền

* **Endpoint:** `GET /permissions`
* **Response mẫu:**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "USER_VIEW",
      "description": "Xem danh sách người dùng",
      "created_at": "2025-12-23T10:00:00Z",
      "created_by": "system"
    }
  ]
}

```

---

## 2. Quản lý Roles (Vai trò)

### 2.1 Tạo Role mới & Gán quyền

* **Endpoint:** `POST /roles`
* **Payload (Request Body):**

```json
{
  "name": "Quản lý kho",
  "code": "WAREHOUSE_MGR",
  "permission_ids": [1, 2, 5]
}

```

### 2.2 Lấy chi tiết Role (Kèm quyền)

* **Endpoint:** `GET /roles/{id}`
* **Response mẫu:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Quản lý kho",
    "code": "WAREHOUSE_MGR",
    "permissions": [
      { "id": 1, "code": "USER_VIEW" },
      { "id": 5, "code": "SCALE_READ" }
    ],
    "created_by": "admin_hoang",
    "updated_at": "2025-12-23T15:00:00Z"
  }
}

```

---

## 3. Quản lý Users (Người dùng)

### 3.1 Tạo User mới & Gán vai trò

* **Endpoint:** `POST /users`
* **Payload:**

```json
{
  "username": "nhanvien_01",
  "password": "SecurePassword123",
  "full_name": "Nguyễn Văn A",
  "status": 1,
  "role_ids": [1, 3]
}

```

### 3.2 Lấy chi tiết User (Kèm Role & Permission)

Đây là API quan trọng nhất theo yêu cầu của bạn: Trả về dữ liệu lồng nhau 3 cấp.

* **Endpoint:** `GET /users/{id}`
* **Response mẫu:**

```json
{
  "success": true,
  "data": {
    "id": 101,
    "username": "nhanvien_01",
    "full_name": "Nguyễn Văn A",
    "status": 1,
    "roles": [
      {
        "id": 1,
        "name": "Quản lý kho",
        "code": "WAREHOUSE_MGR",
        "permissions": [
          { "id": 5, "code": "SCALE_READ", "description": "Đọc dữ liệu cân" },
          { "id": 6, "code": "SCALE_REPORT", "description": "Xem báo cáo" }
        ]
      }
    ],
    "created_at": "2025-12-23T08:00:00Z",
    "created_by": "admin_hoang"
  }
}

```

### 3.3 Cập nhật Role cho User

Dùng để thay đổi chức vụ hoặc gán thêm quyền cho nhân viên.

* **Endpoint:** `PUT /users/{id}/roles`
* **Payload:**

```json
{
  "role_ids": [1, 2, 4] 
}

```

---

## 4. Danh mục mã lỗi (Standard Error Response)

Hệ thống sử dụng các mã HTTP Standard kết hợp Error Code nội bộ:

| HTTP Code | Error Code | Ý nghĩa |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Dữ liệu đầu vào không hợp lệ (ví dụ: thiếu role_ids). |
| 409 | `ALREADY_EXISTS` | Username hoặc Role Code đã tồn tại trong hệ thống. |
| 404 | `NOT_FOUND` | Không tìm thấy ID tương ứng. |
| 500 | `INTERNAL_ERROR` | Lỗi DB hoặc logic xử lý phía server. |

**Cấu trúc lỗi mẫu:**

```json
{
  "success": false,
  "error_code": "ALREADY_EXISTS",
  "message": "Username 'nhanvien_01' đã được sử dụng."
}