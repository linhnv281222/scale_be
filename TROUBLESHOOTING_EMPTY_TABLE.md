# Troubleshooting: Bảng Không Có Dữ Liệu Khi Xuất Báo Cáo

## Các nguyên nhân có thể xảy ra

### 1. ❌ Query không trả về dữ liệu

**Triệu chứng:**
- Log hiển thị: `Queried data: 0 rows`
- Log hiển thị: `Rows list is EMPTY - no data to export!`

**Nguyên nhân:**
- Không có dữ liệu trong khoảng thời gian query
- scaleIds filter không khớp với dữ liệu có sẵn
- Timezone không đúng (startTime/endTime)

**Giải pháp:**
```bash
# Kiểm tra dữ liệu trong DB
SELECT COUNT(*), MIN(time), MAX(time) 
FROM weighing_logs 
WHERE scale_id = 1;

# Kiểm tra với timezone
SELECT * FROM weighing_logs 
WHERE time >= '2025-01-01 00:00:00+07:00'
  AND time <= '2025-01-31 23:59:59+07:00'
LIMIT 10;
```

**Test request:**
```json
{
  "startTime": "2025-01-01T00:00:00+07:00",
  "endTime": "2025-01-31T23:59:59+07:00",
  "scaleIds": null,  // ← Thử null để lấy tất cả
  "type": "WORD"
}
```

---

### 2. ❌ Template Word không đúng cú pháp POI-TL

**Triệu chứng:**
- Log hiển thị: `Queried data: 10 rows` (có data)
- File Word xuất ra trống hoặc chỉ có header

**Nguyên nhân:**
- Cú pháp `{{#rows}}...{{/rows}}` không đúng
- Vị trí placeholder trong bảng Word không đúng
- Sử dụng LoopRowTableRenderPolicy nhưng template không phải table

**Giải pháp:**

#### A. Kiểm tra template có đúng cấu trúc không

Với **LoopRowTableRenderPolicy** (bảng Word):

```
Bảng phải có ít nhất 4 dòng:

Dòng 1: Header (STT | Mã cân | Tên | ...)
Dòng 2: {{#rows}} | (các ô khác để trống hoặc bỏ qua)
Dòng 3: {{rowNumber}} | {{scaleCode}} | {{scaleName}} | ...
Dòng 4: {{/rows}} | (các ô khác để trống hoặc bỏ qua)
```

**QUAN TRỌNG:** 
- `{{#rows}}` phải nằm ở ô đầu tiên của dòng
- `{{/rows}}` phải nằm ở ô đầu tiên của dòng
- Các placeholder data nằm ở dòng GIỮA hai tag trên

#### B. Thử cú pháp đơn giản hơn (không dùng bảng)

Sửa code tạm thời:
```java
// Không dùng LoopRowTableRenderPolicy
Configure config = Configure.builder().build();
```

Template Word không dùng bảng:
```
Danh sách dữ liệu:

{{#rows}}
- STT {{rowNumber}}: {{scaleCode}} - {{scaleName}}
  Data 1: {{data1Total}}, Data 2: {{data2Total}}
{{/rows}}

Tổng: {{summary.totalRecords}} bản ghi
```

---

### 3. ❌ Data model không được chuẩn bị đúng

**Triệu chứng:**
- Log hiển thị: `Data model prepared - rows count: NOT A LIST`
- Hoặc exception khi render template

**Nguyên nhân:**
- convertRowToMap() có lỗi
- reportData.getRows() trả về null

**Giải pháp:**

Kiểm tra log chi tiết:
```
Data model prepared - rows count: 10
Data model keys: [organizationName, reportTitle, rows, summary, ...]
First row sample: {rowNumber=1, scaleCode=TC001, scaleName=Trạm 1, data1Total=1.234,56}
```

Nếu không thấy log này → vấn đề ở ReportExportService.prepareDataModelForTemplate()

---

### 4. ❌ Template file bị lỗi hoặc corrupt

**Triệu chứng:**
- Exception: `Template rendering failed`
- Hoặc file xuất ra có kích thước nhỏ bất thường

**Nguyên nhân:**
- File template .docx bị lỗi
- Chứa ký tự không hợp lệ
- Placeholder có invisible characters

**Giải pháp:**
```bash
# Tạo template mới hoàn toàn từ Word
1. Mở Word → Tạo file mới
2. Gõ trực tiếp các placeholder (không copy-paste)
3. Lưu dưới dạng .docx (Office Open XML)
4. Upload lại vào hệ thống
```

---

### 5. ❌ LoopRowTableRenderPolicy không tương thích

**Triệu chứng:**
- Có data, template đúng, nhưng vẫn không render

**Nguyên nhân:**
- Version POI-TL không tương thích
- Cần cú pháp khác cho table loop

**Giải pháp:**

Thử các cách sau:

#### Cách 1: Inline table syntax
```
| {{#rows}}{{rowNumber}}{{/rows}} | {{#rows}}{{scaleCode}}{{/rows}} | {{#rows}}{{data1Total}}{{/rows}} |
```

#### Cách 2: Sử dụng cú pháp []
```
| [#rows]{{rowNumber}}[/] | [#rows]{{scaleCode}}[/] | [#rows]{{data1Total}}[/] |
```

#### Cách 3: Không dùng policy
```java
// Remove LoopRowTableRenderPolicy
Configure config = Configure.builder().build();
```

Dùng loop thường:
```
{{#rows}}
{{rowNumber}} | {{scaleCode}} | {{data1Total}}
{{/rows}}
```

---

## Quy trình debug từng bước

### Bước 1: Kiểm tra log sau khi export

Tìm các dòng sau trong log (terminal hoặc file log):

```
✅ Queried data: X rows              ← Phải > 0
✅ Data model prepared - rows count: X  ← Phải = số rows ở trên
✅ First row sample: {...}            ← Phải có dữ liệu đầy đủ
✅ Compiling and rendering template... ← Bắt đầu render
✅ Report exported successfully: Y bytes ← Phải > 5KB (tùy data)
```

### Bước 2: Test với template tối giản

Tạo file `test-simple.docx`:
```
TEST TEMPLATE

Số dòng: {{totalLogs}}

{{#rows}}
{{rowNumber}}. {{scaleCode}}
{{/rows}}

DONE
```

Upload và test. Nếu work → vấn đề ở template phức tạp.

### Bước 3: Test với sample data hardcode

Thêm vào ReportExportService (tạm thời để test):
```java
// Test hardcoded data
List<Map<String, Object>> testRows = new ArrayList<>();
for (int i = 1; i <= 5; i++) {
    Map<String, Object> row = new HashMap<>();
    row.put("rowNumber", i);
    row.put("scaleCode", "TC00" + i);
    row.put("scaleName", "Test " + i);
    row.put("data1Total", "1.000,00");
    testRows.add(row);
}
dataModel.put("rows", testRows);
log.info("USING TEST DATA: 5 rows");
```

Nếu test data work → vấn đề ở query hoặc data transformation.

### Bước 4: Kiểm tra file Word output

```bash
# Kiểm tra kích thước file
Get-ChildItem report*.docx | Select-Object Name, Length

# Nếu < 5KB → có vấn đề
# Nếu > 20KB → có thể đã có data
```

Mở file Word và kiểm tra:
- Có hiển thị header information không?
- Có hiển thị summary không?
- Chỉ thiếu bảng data?

### Bước 5: Enable debug logging

Thêm vào `application.properties`:
```properties
logging.level.org.facenet.service.report=DEBUG
logging.level.com.deepoove.poi=DEBUG
```

Restart app và xem log chi tiết của POI-TL.

---

## Checklist nhanh

- [ ] Database có dữ liệu trong khoảng thời gian query
- [ ] Log hiển thị "Queried data: X rows" với X > 0
- [ ] Log hiển thị "rows count: X" (không phải "NOT A LIST")
- [ ] Log hiển thị "First row sample" với đầy đủ fields
- [ ] Template Word có cú pháp đúng: {{#rows}}...{{/rows}}
- [ ] Template Word là file .docx chuẩn (không bị corrupt)
- [ ] LoopRowTableRenderPolicy được config nếu dùng bảng Word
- [ ] Không có exception trong log khi render template
- [ ] File output > 5KB

---

## Contact

Nếu vẫn gặp vấn đề sau khi check hết các bước trên:
1. Share log đầy đủ khi export (từ "Exporting report" đến "Report exported")
2. Share template Word file (.docx)
3. Share sample request JSON
4. Share screenshot file Word output (nếu có)
