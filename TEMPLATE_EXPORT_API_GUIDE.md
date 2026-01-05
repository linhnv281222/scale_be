# Template Export API - Hướng Dẫn Sử Dụng

## Tổng quan

API mới cho phép xuất báo cáo trực tiếp bằng cách sử dụng template đã import, không cần dùng đến report code hay report definition.

## Luồng hoạt động

```
1. Import template file (.docx) → Lưu vào bảng template_imports
2. Gọi API export với templateId + filters
3. Hệ thống truy vấn dữ liệu theo filters
4. Fill dữ liệu vào template
5. Trả về file Word đã điền dữ liệu
```

## API Endpoint

### Export Report với Template Import (MỚI - KHUYÊN DÙNG)

**POST** `/api/v1/reports/export`

#### Query Parameters
- `importId` (required): ID của template import từ bảng `template_imports` (không phải templateId)

#### Request Body
```json
{
  "startTime": "2025-01-01T00:00:00+07:00",
  "endTime": "2025-01-31T23:59:59+07:00",
  "scaleIds": [1, 2, 3],
  "reportTitle": "BÁO CÁO SẢN LƯỢNG THÁNG 01/2025",
  "reportCode": "BCSL_T01_2025",
  "preparedBy": "Nguyễn Văn A",
  "type": "WORD"
}
```

#### Response Headers
- `Content-Disposition`: attachment; filename="..."
- `X-Import-Id`: ID của template import đã sử dụng
- `X-Execution-Time-Ms`: Thời gian thực thi (ms)
- `Content-Type`: application/vnd.openxmlformats-officedocument.wordprocessingml.document

#### Response Body
Binary data của file Word (.docx)

## Ví dụ sử dụng

### 1. Import Template trước

```bash
POST /api/v1/report-templates/import
Content-Type: multipart/form-data

file: my-template.docx
templateCode: BCSL_MONTHLY
templateName: Báo cáo sản lượng tháng
description: Template báo cáo sản lượng hàng tháng
```

Response:
```json
{
  "success": true,
  "data": {
    "id": 5,
    "templateId": 12,
    "templateCode": "BCSL_MONTHLY",
    "originalFilename": "my-template.docx",
    "resourcePath": "templates/reports/BCSL_MONTHLY_20250131_143022.docx",
    ...
  }
}
```

Lưu lại `id = 5` (đây là importId) để dùng cho export.

### 2. Export báo cáo với template

```bash
POST /api/v1/reports/export?importId=5
Content-Type: application/json

{
  "startTime": "2025-01-01T00:00:00+07:00",
  "endTime": "2025-01-31T23:59:59+07:00",
  "scaleIds": [1, 2],
  "reportTitle": "BÁO CÁO SẢN LƯỢNG THÁNG 01/2025",
  "reportCode": "BCSL_T01_2025",
  "preparedBy": "Admin",
  "type": "WORD"
}
```

Response: File Word binary

## Template Variables

Template file (.docx) cần có các placeholders sau để hệ thống fill dữ liệu:

### Header Fields
- `{{reportTitle}}` - Tiêu đề báo cáo
- `{{reportCode}}` - Mã báo cáo
- `{{exportTime}}` - Thời gian xuất (dd/MM/yyyy HH:mm:ss)
- `{{startTime}}` - Ngày bắt đầu (dd/MM/yyyy)
- `{{endTime}}` - Ngày kết thúc (dd/MM/yyyy)
- `{{preparedBy}}` - Người xuất báo cáo
- `{{scaleNames}}` - Danh sách trạm cân

### Column Names (Configurable)
- `{{data1Name}}` - Tên cột dữ liệu 1 (từ scale_config)
- `{{data2Name}}` - Tên cột dữ liệu 2 (từ scale_config)
- `{{data3Name}}` - Tên cột dữ liệu 3 (từ scale_config)
- `{{data4Name}}` - Tên cột dữ liệu 4 (từ scale_config)
- `{{data5Name}}` - Tên cột dữ liệu 5 (từ scale_config)

### Table Data (Loop)
```
{{#rows}}
- {{rowNumber}} - Số thứ tự
- {{scaleId}} - ID trạm cân
- {{scaleCode}} - Mã trạm cân
- {{scaleName}} - Tên trạm cân
- {{location}} - Vị trí
- {{period}} - Thời điểm (theo nhóm thời gian)
- {{data1Total}} - Tổng dữ liệu 1 (đã format)
- {{data2Total}} - Tổng dữ liệu 2 (đã format)
- {{data3Total}} - Tổng dữ liệu 3 (đã format)
- {{data4Total}} - Tổng dữ liệu 4 (đã format)
- {{data5Total}} - Tổng dữ liệu 5 (đã format)
- {{recordCount}} - Số bản ghi
- {{lastTime}} - Thời gian cuối cùng (dd/MM/yyyy HH:mm:ss)
{{/rows}}
```

### Summary Fields
- `{{totalScales}}` - Tổng số trạm cân
- `{{totalRecords}}` - Tổng số bản ghi
- `{{data1GrandTotal}}` - Tổng lớn dữ liệu 1 (đã format)
- `{{data2GrandTotal}}` - Tổng lớn dữ liệu 2 (đã format)
- `{{data3GrandTotal}}` - Tổng lớn dữ liệu 3 (đã format)
- `{{data4GrandTotal}}` - Tổng lớn dữ liệu 4 (đã format)
- `{{data5GrandTotal}}` - Tổng lớn dữ liệu 5 (đã format)

## Ví dụ Template Word

```
CÔNG TY ABC
------------------

{{reportTitle}}
Mã báo cáo: {{reportCode}}

Thời gian xuất: {{exportTime}}
Khoảng thời gian: {{startTime}} - {{endTime}}
Người xuất: {{preparedBy}}
Trạm cân: {{scaleNames}}

| STT | Trạm cân | Thời điểm | {{data1Name}} | {{data2Name}} | Số bản ghi |
|-----|----------|-----------|---------------|---------------|------------|
{{#rows}}
| {{rowNumber}} | {{scaleName}} | {{period}} | {{data1Total}} | {{data2Total}} | {{recordCount}} |
{{/rows}}

TỔNG KẾT:
- Tổng trạm cân: {{totalScales}}
- Tổng bản ghi: {{totalRecords}}
- Tổng {{data1Name}}: {{data1GrandTotal}}
- Tổng {{data2Name}}: {{data2GrandTotal}}
```

## So sánh API cũ vs mới

### API Cũ (LEGACY - Vẫn hoạt động)
```
POST /api/v1/reports/{reportCode}/export?format=WORD
- Cần định nghĩa report code trước
- Cần cấu hình report definition
- Phức tạp hơn
```

### API Mới (KHUYÊN DÙNG)
```
POST /api/v1/reports/export?importId=5
- Chỉ cần import template
- Đơn giản, linh hoạt
- Trực tiếp fill data vào template
- importId là ID từ bảng template_imports
```

## Lưu ý

1. **Template phải là file Word (.docx)** - Hỗ trợ định dạng OpenXML
2. **Template ID phải tồn tại** - Kiểm tra trong bảng `template_imports`
3. **Định dạng số** - Sử dụng locale VN (dấu chấm ngăn cách hàng nghìn, dấu phẩy cho thập phân)
4. **Định dạng ngày** - dd/MM/yyyy hoặc dd/MM/yyyy HH:mm:ss

## Troubleshooting

### Lỗi "Template import not found"
- Kiểm tra `importId` có đúng không (phải là ID từ bảng `template_imports`, không phải `template_id`)
- Kiểm tra template có active không: `SELECT * FROM template_imports WHERE id = ?`
- Xem danh sách template imports: `GET /api/v1/report-templates/imports`

### Template không fill data
- Kiểm tra placeholder trong template có đúng không
- Xem log để check data model: "Prepared data model: X rows"

### File bị lỗi format
- Đảm bảo template gốc là file .docx hợp lệ
- Thử mở template với Microsoft Word/LibreOffice

## API Endpoints liên quan

- `GET /api/v1/report-templates/imports` - Danh sách template đã import
- `GET /api/v1/report-templates/imports/{importId}` - Chi tiết template import
- `GET /api/v1/report-templates/imports/by-template/{templateId}` - Lấy import theo templateId
- `GET /api/v1/report-templates/imports/{importId}/download` - Download template file gốc
