# ScaleHubIOT – Hướng dẫn luồng cấu hình Template báo cáo (Word)

Tài liệu này mô tả **luồng mới** để cấu hình và sử dụng **Word Report Template (.docx)** thông qua API.

## 1) Tổng quan luồng

### Mục tiêu
- Cho phép quản trị viên tạo/cập nhật **Report Template kiểu WORD**.
- Upload/download file **.docx** cho template.
- Khi export báo cáo, hệ thống sẽ **ưu tiên dùng DOCX đã cấu hình** theo `templateId` (nếu có), nếu không sẽ fallback.

### Thứ tự ưu tiên template khi export Word
Trong `WordExportService`, hệ thống chọn template theo thứ tự:
1. **DOCX đã upload** trong DB của template được chọn (theo `templateId` trong request export).
2. **Template DOCX mặc định trong classpath** (đường dẫn `templates/enterprise-report-template.docx` nếu có).
3. Nếu không có DOCX, hệ thống **tự generate Word** (programmatic).

## 2) Base URL / Auth

- Base path API: `http(s)://<host>:<port>/api/v1`
- Các API dưới đây tuân theo cơ chế bảo mật JWT/RBAC hiện có của hệ thống.

> Gợi ý: khi test bằng curl, thêm header: `Authorization: Bearer <TOKEN>`

## 3) API quản lý Word Templates

### 3.1 List templates
- `GET /report-templates/word?activeOnly=false`

Ví dụ:
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/report-templates/word?activeOnly=false"
```

### 3.2 Get template by id
- `GET /report-templates/word/{id}`

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/report-templates/word/1"
```

### 3.3 Create template
- `POST /report-templates/word`
- Body JSON:

```json
{
  "code": "ENTERPRISE_WORD_V1",
  "name": "Template Word doanh nghiệp v1",
  "description": "Template chuẩn xuất báo cáo",
  "titleTemplate": "BÁO CÁO TỔNG HỢP",
  "isActive": true,
  "isDefault": false
}
```

Ví dụ:
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"code":"ENTERPRISE_WORD_V1","name":"Template Word doanh nghiệp v1","isActive":true,"isDefault":false}' \
  "http://localhost:8080/api/v1/report-templates/word"
```

### 3.4 Update template
- `PUT /report-templates/word/{id}`

```json
{
  "name": "Template Word doanh nghiệp v1.1",
  "description": "Update format",
  "titleTemplate": "BÁO CÁO DOANH NGHIỆP",
  "isActive": true
}
```

### 3.5 Set default template (WORD)
- `POST /report-templates/word/{id}/set-default`

Ghi chú:
- Hệ thống sẽ tự **unset** `isDefault` của template WORD khác.

### 3.6 Upload file DOCX cho template
- `POST /report-templates/word/{id}/file`
- Content-Type: `multipart/form-data`
- Field: `file` (chỉ chấp nhận `.docx`)

Ví dụ:
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -F "file=@./my-template.docx" \
  "http://localhost:8080/api/v1/report-templates/word/1/file"
```

### 3.7 Download file DOCX của template
- `GET /report-templates/word/{id}/file`

```bash
curl -L -H "Authorization: Bearer $TOKEN" \
  -o downloaded-template.docx \
  "http://localhost:8080/api/v1/report-templates/word/1/file"
```

### 3.8 Delete template
- `DELETE /report-templates/word/{id}`

```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/report-templates/word/1"
```

## 4) Export báo cáo Word với template đã cấu hình

Hiện tại các endpoint export đã có hỗ trợ tham số `templateId`.

- `POST /reports/export` (hoặc endpoint enterprise export tương ứng)
- Trong payload export, truyền `templateId` để chọn template.

Ví dụ payload (minh hoạ):
```json
{
  "fromDate": "2025-12-01T00:00:00+07:00",
  "toDate": "2025-12-29T23:59:59+07:00",
  "format": "WORD",
  "templateId": 1,
  "aggregationMethod": "SUM"
}
```

Khi `templateId` trỏ tới template WORD có file DOCX đã upload, hệ thống sẽ dùng DOCX đó để render.

## 5) Lưu ý về DB schema

Luồng mới lưu file DOCX trực tiếp trong bảng template (dạng `byte[]`/LOB):
- `word_template_filename`
- `word_template_content`

Nếu môi trường PROD không bật auto-DDL, cần tạo migration tương ứng (tuỳ cách bạn quản lý schema).

## 6) Mapping placeholder trong DOCX (gợi ý)

Hệ thống đang render Word qua poi-tl/XWPF. Template DOCX nên dùng các placeholder/tables theo hướng dẫn nội bộ đã có:
- Xem thêm: `src/main/resources/templates/WORD_TEMPLATE_GUIDE.md`

## 7) Quick test checklist

- Tạo template WORD (POST)
- Upload file `.docx`
- Export báo cáo truyền `templateId`
- Download file `.docx` để đối chiếu
- (Tuỳ chọn) Set default template và export không truyền `templateId` (nếu luồng export của bạn có fallback default)
