# Tài Liệu Template Keys - ScaleHub IoT Report Export

## Tổng Quan

Tài liệu này mô tả chi tiết **tất cả các key (placeholders)** có thể sử dụng trong các template báo cáo của hệ thống ScaleHub IoT. Hệ thống hỗ trợ 3 loại template:

- **Word Template (.docx)** - Sử dụng Apache POI XWPF
- **PDF Template (.html)** - Sử dụng Thymeleaf + Flying Saucer
- **Excel Template (.xlsx)** - Sử dụng Apache POI XSSF

## Cú Pháp Placeholder

### Word Template (.docx)
```
{{keyName}}
{{summary.totalRecords}}
```

### PDF Template (.html)
```html
<span th:text="${keyName}">Default value</span>
<span th:text="${summary.totalRecords}">0</span>
```

### Excel Template (.xlsx)
```
{{keyName}}
{{summary.totalRecords}}
```

---

## 1. Thông Tin Tổ Chức (Organization Information)

Các thông tin về công ty/tổ chức được lấy từ bảng `organization_settings`.

| Key | Kiểu Dữ Liệu | Mô Tả | Ví Dụ |
|-----|--------------|-------|--------|
| `organizationName` | String | Tên công ty (tiếng Việt) | "Công Ty TNHH ABC" |
| `companyNameEn` | String | Tên công ty (tiếng Anh) | "ABC Co., Ltd" |
| `address` | String | Địa chỉ công ty | "123 Nguyễn Văn A, Q1, TP.HCM" |
| `phone` | String | Số điện thoại | "028-12345678" |
| `email` | String | Email liên hệ | "contact@company.com" |
| `taxCode` | String | Mã số thuế | "0123456789" |
| **`hasLogo`** | Boolean | Logo có tồn tại hay không | `true` / `false` |

### Logo trong Template

Logo của tổ chức được tự động load và có thể hiển thị trong template.

#### **Word Template (.docx)**
Sử dụng placeholder đặc biệt `[[LOGO]]` trong paragraph:

```
[[LOGO]]
```

Logo sẽ được chèn tự động với kích thước mặc định (150x50 pixels). Đặt placeholder này ở bất kỳ đâu trong document (không phải trong table).

#### **PDF/HTML Template (.html)**
Sử dụng Thymeleaf với biến `logoBase64`:

```html
<!-- Logo with Base64 data URI -->
<img th:if="${hasLogo}" th:src="${logoBase64}" style="width: 150px; height: auto;" />

<!-- Conditional display -->
<div th:if="${hasLogo}">
    <img th:src="${logoBase64}" alt="Company Logo" style="max-width: 200px;" />
</div>

<!-- With fallback -->
<img th:src="${hasLogo} ? ${logoBase64} : '/images/default-logo.png'" 
     alt="Logo" 
     style="width: 150px; height: 50px; object-fit: contain;" />
```

Biến `logoBase64` chứa data URI đầy đủ: `data:image/png;base64,iVBORw0KGgo...`

#### **Excel Template (.xlsx)**
Excel không hỗ trợ tự động insert image từ placeholder. Logo sẽ được bỏ qua trong Excel export.

**Ví dụ sử dụng:**

**Word Template - Header với Logo:**
```
[[LOGO]]

{{organizationName}}
Địa chỉ: {{address}}
ĐT: {{phone}} - Email: {{email}}
Mã số thuế: {{taxCode}}
```

**HTML Template - Header với Logo:**
```html
<div class="header">
    <div th:if="${hasLogo}">
        <img th:src="${logoBase64}" alt="Company Logo" style="width: 150px; height: auto;" />
    </div>
    <h2 th:text="${organizationName}">Company Name</h2>
    <p>Địa chỉ: <span th:text="${address}">Address</span></p>
    <p>ĐT: <span th:text="${phone}">Phone</span> - Email: <span th:text="${email}">Email</span></p>
</div>
```

**Word:**
```
Công ty: {{organizationName}}
Địa chỉ: {{address}}
ĐT: {{phone}} - Email: {{email}}
```

**HTML:**
```html
<p>Công ty: <span th:text="${organizationName}">ABC Company</span></p>
<p>Địa chỉ: <span th:text="${address}">123 Street</span></p>
```

**Excel:**
```
A1: Công ty: {{organizationName}}
A2: Địa chỉ: {{address}}
```

---

## 2. Thông Tin Báo Cáo (Report Header)

Thông tin cơ bản về báo cáo.

| Key | Kiểu Dữ Liệu | Mô Tả | Ví Dụ |
|-----|--------------|-------|--------|
| `reportTitle` | String | Tiêu đề báo cáo | "BÁO CÁO CÂN TỔNG HỢP" |
| `reportCode` | String | Mã báo cáo | "BCSL-001" |
| `startTime` | String | Thời gian bắt đầu (định dạng: dd/MM/yyyy) | "01/01/2026" |
| `endTime` | String | Thời gian kết thúc (định dạng: dd/MM/yyyy) | "31/01/2026" |
| `exportTime` | String | Thời gian xuất báo cáo (định dạng: dd/MM/yyyy HH:mm:ss) | "09/01/2026 14:30:25" |
| `currentDateTime` | String | Thời gian hiện tại (định dạng: dd/MM/yyyy HH:mm:ss) | "09/01/2026 14:30:25" |
| `preparedBy` | String | Người lập báo cáo | "Nguyễn Văn A" |
| `scaleNames` | String | Danh sách tên cân (phân cách bằng dấu phẩy) | "Cân 1, Cân 2, Cân 3" |

**Ví dụ sử dụng:**

**Word:**
```
{{reportTitle}}
Mã báo cáo: {{reportCode}}
Từ ngày: {{startTime}} - Đến ngày: {{endTime}}
Người lập: {{preparedBy}}
Ngày xuất: {{exportTime}}
```

**HTML:**
```html
<h1 th:text="${reportTitle}">Báo cáo</h1>
<p>Mã báo cáo: <span th:text="${reportCode}">BCSL-001</span></p>
<p>Từ ngày: <span th:text="${startTime}">01/01/2026</span> 
   - Đến ngày: <span th:text="${endTime}">31/01/2026</span></p>
```

---

## 3. Tên Cột Dữ Liệu (Column Names)

Tên các cột dữ liệu động được lấy từ cấu hình của cân (`scale_configs`).

| Key | Kiểu Dữ Liệu | Mô Tả | Mặc Định |
|-----|--------------|-------|----------|
| `data1Name` | String | Tên cột dữ liệu 1 | "Data 1" |
| `data2Name` | String | Tên cột dữ liệu 2 | "Data 2" |
| `data3Name` | String | Tên cột dữ liệu 3 | "Data 3" |
| `data4Name` | String | Tên cột dữ liệu 4 | "Data 4" |
| `data5Name` | String | Tên cột dữ liệu 5 | "Data 5" |

**Ví dụ:** Nếu scale_configs có `data1_name="Khối lượng"`, thì `{{data1Name}}` sẽ hiển thị "Khối lượng".

**Ví dụ sử dụng trong header bảng:**

**Word:**
```
| STT | Tên cân | {{data1Name}} | {{data2Name}} | {{data3Name}} |
```

**HTML:**
```html
<th th:text="${data1Name}">Data 1</th>
<th th:text="${data2Name}">Data 2</th>
```

---

## 4. Dữ Liệu Bảng (Table Rows)

Mảng các dòng dữ liệu cho bảng chính. Sử dụng để lặp qua từng dòng.

### Key Chính

| Key | Kiểu Dữ Liệu | Mô Tả |
|-----|--------------|-------|
| `rows` | List&lt;Map&gt; | Danh sách các dòng dữ liệu |

### Thuộc Tính của Mỗi Dòng (Row)

Mỗi phần tử trong `rows` là một Map với các key sau:

| Key | Kiểu Dữ Liệu | Mô Tả | Ví Dụ |
|-----|--------------|-------|--------|
| `rowNumber` | Integer | Số thứ tự | 1, 2, 3... |
| `scaleId` | Long | ID của cân | 123 |
| `scaleCode` | String | Mã cân | "CAN-001" |
| `scaleName` | String | Tên cân | "Cân 1" |
| `location` | String | Vị trí cân | "Nhà máy A" |
| `period` | String | Khoảng thời gian (nếu group by time) | "2026-01-09 14:00" hoặc "2026-01-09" |
| `data1Total` | String | Tổng dữ liệu 1 (đã format số) | "1.234,56" |
| `data2Total` | String | Tổng dữ liệu 2 (đã format số) | "5.678,90" |
| `data3Total` | String | Tổng dữ liệu 3 (đã format số) | "9.012,34" |
| `data4Total` | String | Tổng dữ liệu 4 (đã format số) | "3.456,78" |
| `data5Total` | String | Tổng dữ liệu 5 (đã format số) | "7.890,12" |
| `recordCount` | Integer | Số lượng bản ghi | 150 |
| `lastTime` | String | Thời gian cân cuối cùng (định dạng: dd/MM/yyyy HH:mm:ss) | "09/01/2026 14:25:30" |

**Định Dạng Số:** Tất cả các giá trị `dataXTotal` đã được format theo chuẩn Việt Nam:
- Dấu phân cách hàng nghìn: `.` (dấu chấm)
- Dấu phân cách thập phân: `,` (dấu phẩy)
- Số chữ số thập phân: 2

### Cách Sử Dụng trong Template

#### Word Template (.docx)

**Cấu trúc bảng cần có:**
1. Dòng 0: Header (tiêu đề cột)
2. Dòng 1: Template row (chứa placeholder, sẽ bị xóa sau khi xử lý)

**Ví dụ:**

| STT | Mã cân | Tên cân | Vị trí | {{data1Name}} | {{data2Name}} | Số lần | Thời gian cuối |
|-----|--------|---------|--------|---------------|---------------|--------|----------------|
| {{rowNumber}} | {{scaleCode}} | {{scaleName}} | {{location}} | {{data1Total}} | {{data2Total}} | {{recordCount}} | {{lastTime}} |
| (Các dòng này sẽ được tự động tạo từ dữ liệu) | | | | | | | |

**Lưu ý quan trọng:**
- Hệ thống tự động nhận diện placeholder trong dòng template
- Dòng template sẽ được nhân bản cho mỗi dòng dữ liệu
- Sau khi xử lý, dòng template gốc sẽ bị xóa

#### HTML Template (.html) cho PDF

```html
<table>
  <thead>
    <tr>
      <th>STT</th>
      <th>Mã cân</th>
      <th>Tên cân</th>
      <th th:text="${data1Name}">Data 1</th>
      <th th:text="${data2Name}">Data 2</th>
      <th>Số lần</th>
    </tr>
  </thead>
  <tbody>
    <tr th:each="row, stat : ${rows}">
      <td th:text="${row.rowNumber}">1</td>
      <td th:text="${row.scaleCode}">CAN-001</td>
      <td th:text="${row.scaleName}">Cân 1</td>
      <td th:text="${row.data1Total}">1,234.56</td>
      <td th:text="${row.data2Total}">5,678.90</td>
      <td th:text="${row.recordCount}">150</td>
    </tr>
  </tbody>
</table>
```

#### Excel Template (.xlsx)

**Cấu trúc tương tự Word:**

| A | B | C | D | E | F |
|---|---|---|---|---|---|
| STT | Mã cân | Tên cân | {{data1Name}} | {{data2Name}} | Số lần |
| {{rowNumber}} | {{scaleCode}} | {{scaleName}} | {{data1Total}} | {{data2Total}} | {{recordCount}} |
| (Các dòng tự động) | | | | | |

---

## 5. Thông Tin Tổng Hợp (Summary Statistics)

Các số liệu thống kê tổng hợp. Tất cả các giá trị số đã được format.

### Cú Pháp

Sử dụng notation `.` để truy cập các thuộc tính của summary:

```
{{summary.totalRecords}}
{{summary.data1GrandTotal}}
```

### Danh Sách Key Summary

| Key | Kiểu Dữ Liệu | Mô Tả | Ví Dụ |
|-----|--------------|-------|--------|
| `summary.totalScales` | Integer | Tổng số cân | 5 |
| `summary.totalRecords` | Integer | Tổng số bản ghi | 1500 |
| `summary.data1GrandTotal` | String | Tổng cộng Data 1 (đã format) | "123.456,78" |
| `summary.data2GrandTotal` | String | Tổng cộng Data 2 (đã format) | "234.567,89" |
| `summary.data3GrandTotal` | String | Tổng cộng Data 3 (đã format) | "345.678,90" |
| `summary.data4GrandTotal` | String | Tổng cộng Data 4 (đã format) | "456.789,01" |
| `summary.data5GrandTotal` | String | Tổng cộng Data 5 (đã format) | "567.890,12" |
| `summary.data1Average` | String | Trung bình Data 1 (đã format) | "82,30" |
| `summary.data2Average` | String | Trung bình Data 2 (đã format) | "156,38" |
| `summary.data3Average` | String | Trung bình Data 3 (đã format) | "230,45" |
| `summary.data4Average` | String | Trung bình Data 4 (đã format) | "304,53" |
| `summary.data5Average` | String | Trung bình Data 5 (đã format) | "378,59" |
| `summary.data1Max` | String | Giá trị lớn nhất Data 1 (đã format) | "999,99" |
| `summary.data2Max` | String | Giá trị lớn nhất Data 2 (đã format) | "1.234,56" |
| `summary.data3Max` | String | Giá trị lớn nhất Data 3 (đã format) | "2.345,67" |
| `summary.data4Max` | String | Giá trị lớn nhất Data 4 (đã format) | "3.456,78" |
| `summary.data5Max` | String | Giá trị lớn nhất Data 5 (đã format) | "4.567,89" |

**Ví dụ sử dụng:**

**Word:**
```
TỔNG KẾT
─────────────────────────────────────────
Tổng số cân: {{summary.totalScales}}
Tổng số lần cân: {{summary.totalRecords}}
Tổng {{data1Name}}: {{summary.data1GrandTotal}}
Tổng {{data2Name}}: {{summary.data2GrandTotal}}
Trung bình {{data1Name}}: {{summary.data1Average}}
```

**HTML:**
```html
<div class="summary">
  <h3>TỔNG KẾT</h3>
  <p>Tổng số cân: <strong th:text="${summary.totalScales}">5</strong></p>
  <p>Tổng số lần cân: <strong th:text="${summary.totalRecords}">1500</strong></p>
  <p>Tổng <span th:text="${data1Name}">Data 1</span>: 
     <strong th:text="${summary.data1GrandTotal}">123,456.78</strong></p>
</div>
```

**Excel:**
```
A20: Tổng số cân:
B20: {{summary.totalScales}}
A21: Tổng số lần cân:
B21: {{summary.totalRecords}}
A22: Tổng {{data1Name}}:
B22: {{summary.data1GrandTotal}}
```

---

## 6. Metadata và Các Key Bổ Sung

Các key khác có thể hữu ích trong template.

| Key | Kiểu Dữ Liệu | Mô Tả | Ví Dụ |
|-----|--------------|-------|--------|
| `totalLogs` | Integer | Tổng số dòng dữ liệu (bằng rows.size()) | 50 |
| `watermark` | String | Watermark cho báo cáo | "Generated by ScaleHub IoT" |
| `aggregationMethodLabel` | String | Nhãn phương pháp tổng hợp | "Standard" |

---

## 7. Ví Dụ Template Hoàn Chỉnh

### 7.1. Word Template (.docx)

```
╔════════════════════════════════════════════════════════════════╗
║                                                                 ║
║              {{organizationName}}                               ║
║              {{address}}                                        ║
║              ĐT: {{phone}} - Email: {{email}}                  ║
║                                                                 ║
╚════════════════════════════════════════════════════════════════╝

                    {{reportTitle}}
                 Mã báo cáo: {{reportCode}}
        Từ ngày {{startTime}} đến ngày {{endTime}}

Cân: {{scaleNames}}
Người lập: {{preparedBy}}
Ngày xuất: {{exportTime}}

┌───┬──────────┬─────────────┬─────────────┬──────────────┬──────────────┬─────────┐
│STT│ Mã cân   │ Tên cân     │ {{data1Name}}│ {{data2Name}}│ Số lần      │Thời gian│
├───┼──────────┼─────────────┼─────────────┼──────────────┼──────────────┼─────────┤
│{{rowNumber}}│{{scaleCode}}│{{scaleName}}│{{data1Total}}│{{data2Total}}│{{recordCount}}│{{lastTime}}│
└───┴──────────┴─────────────┴─────────────┴──────────────┴──────────────┴─────────┘

TỔNG KẾT:
─────────────────────────────────────────
Tổng số cân: {{summary.totalScales}}
Tổng số lần cân: {{summary.totalRecords}}
Tổng {{data1Name}}: {{summary.data1GrandTotal}}
Tổng {{data2Name}}: {{summary.data2GrandTotal}}
Trung bình {{data1Name}}: {{summary.data1Average}}
Giá trị max {{data1Name}}: {{summary.data1Max}}
```

### 7.2. HTML Template (.html) cho PDF

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title th:text="${reportTitle}">Báo cáo</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .header { text-align: center; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #000; padding: 5px; }
        th { background-color: #f0f0f0; }
        .summary { margin-top: 20px; }
    </style>
</head>
<body>
    <div class="header">
        <h2 th:text="${organizationName}">Công ty</h2>
        <p th:text="${address}">Địa chỉ</p>
        <p>ĐT: <span th:text="${phone}">Phone</span> - Email: <span th:text="${email}">Email</span></p>
    </div>
    
    <h1 th:text="${reportTitle}">BÁO CÁO</h1>
    <p>Mã báo cáo: <span th:text="${reportCode}">BCSL-001</span></p>
    <p>Từ ngày <span th:text="${startTime}">01/01/2026</span> 
       đến ngày <span th:text="${endTime}">31/01/2026</span></p>
    <p>Cân: <span th:text="${scaleNames}">Tất cả</span></p>
    <p>Người lập: <span th:text="${preparedBy}">Admin</span></p>
    
    <table>
        <thead>
            <tr>
                <th>STT</th>
                <th>Mã cân</th>
                <th>Tên cân</th>
                <th>Vị trí</th>
                <th th:text="${data1Name}">Data 1</th>
                <th th:text="${data2Name}">Data 2</th>
                <th>Số lần</th>
                <th>Thời gian cuối</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="row : ${rows}">
                <td th:text="${row.rowNumber}">1</td>
                <td th:text="${row.scaleCode}">CAN-001</td>
                <td th:text="${row.scaleName}">Cân 1</td>
                <td th:text="${row.location}">Nhà máy A</td>
                <td th:text="${row.data1Total}">1,234.56</td>
                <td th:text="${row.data2Total}">5,678.90</td>
                <td th:text="${row.recordCount}">150</td>
                <td th:text="${row.lastTime}">09/01/2026 14:25:30</td>
            </tr>
        </tbody>
    </table>
    
    <div class="summary">
        <h3>TỔNG KẾT</h3>
        <p>Tổng số cân: <strong th:text="${summary.totalScales}">5</strong></p>
        <p>Tổng số lần cân: <strong th:text="${summary.totalRecords}">1500</strong></p>
        <p>Tổng <span th:text="${data1Name}">Data 1</span>: 
           <strong th:text="${summary.data1GrandTotal}">123,456.78</strong></p>
        <p>Tổng <span th:text="${data2Name}">Data 2</span>: 
           <strong th:text="${summary.data2GrandTotal}">234,567.89</strong></p>
        <p>Trung bình <span th:text="${data1Name}">Data 1</span>: 
           <strong th:text="${summary.data1Average}">82.30</strong></p>
    </div>
    
    <div style="margin-top: 30px; text-align: right;">
        <p>Ngày xuất: <span th:text="${exportTime}">09/01/2026 14:30:25</span></p>
        <p th:text="${watermark}">Generated by ScaleHub IoT</p>
    </div>
</body>
</html>
```

### 7.3. Excel Template (.xlsx)

**Tạo file Excel với cấu trúc sau:**

| A | B | C | D | E | F | G | H |
|---|---|---|---|---|---|---|---|
| **{{organizationName}}** ||||||| |
| **{{address}}** ||||||| |
| ĐT: {{phone}} - Email: {{email}} ||||||| |
| ||||||| |
| **{{reportTitle}}** ||||||| |
| Mã: {{reportCode}} | | Từ: {{startTime}} | Đến: {{endTime}} |||| |
| Cân: {{scaleNames}} ||||||| |
| Người lập: {{preparedBy}} | | Ngày xuất: {{exportTime}} |||| |
| ||||||| |
| **STT** | **Mã cân** | **Tên cân** | **Vị trí** | **{{data1Name}}** | **{{data2Name}}** | **Số lần** | **Thời gian cuối** |
| {{rowNumber}} | {{scaleCode}} | {{scaleName}} | {{location}} | {{data1Total}} | {{data2Total}} | {{recordCount}} | {{lastTime}} |
| (Dữ liệu tự động được chèn vào đây) ||||||||
| ||||||| |
| **TỔNG KẾT** ||||||| |
| Tổng số cân: | {{summary.totalScales}} |||||| |
| Tổng số lần cân: | {{summary.totalRecords}} |||||| |
| Tổng {{data1Name}}: | {{summary.data1GrandTotal}} |||||| |
| Tổng {{data2Name}}: | {{summary.data2GrandTotal}} |||||| |
| Trung bình {{data1Name}}: | {{summary.data1Average}} |||||| |

---

## 8. Lưu Ý Quan Trọng

### 8.1. Định Dạng Số
- Tất cả số trong `dataXTotal`, `summary.*Total`, `summary.*Average`, `summary.*Max` đã được format
- Định dạng: `#,##0.00` với locale tiếng Việt
- Dấu phân cách hàng nghìn: `.` (chấm)
- Dấu phân cách thập phân: `,` (phẩy)
- Ví dụ: 1234567.89 → "1.234.567,89"

### 8.2. Định Dạng Thời Gian
- **Ngày**: `dd/MM/yyyy` - Ví dụ: "09/01/2026"
- **Ngày giờ**: `dd/MM/yyyy HH:mm:ss` - Ví dụ: "09/01/2026 14:30:25"

### 8.3. Word Template
- Dòng đầu tiên của bảng PHẢI là header
- Dòng thứ hai PHẢI chứa placeholder (template row)
- Template row sẽ tự động bị xóa sau khi xử lý
- Placeholder có thể đặt ở bất kỳ vị trí nào trong cell

### 8.4. HTML Template
- Sử dụng Thymeleaf syntax (`th:text`, `th:each`)
- Vòng lặp: `th:each="row : ${rows}"`
- Truy cập thuộc tính: `${row.scaleCode}`

### 8.5. Excel Template
- Hệ thống tự động tìm dòng template (chứa `{{rowNumber}}` hoặc placeholder khác)
- Dòng template sẽ được nhân bản và điền dữ liệu
- Sau khi xử lý, dòng template gốc sẽ bị xóa
- Style của template row sẽ được sao chép sang các dòng mới

### 8.6. Giá Trị Null/Empty
- Nếu key không có dữ liệu, sẽ hiển thị chuỗi rỗng `""`
- Số liệu null sẽ hiển thị là `"0"` hoặc `"0,00"` sau khi format

---

## 9. API Endpoint Sử Dụng

### Export với Template Đã Import

```http
POST /api/v1/enterprise/reports/export-with-template/{importId}
Content-Type: application/json

{
  "startTime": "2026-01-01T00:00:00+07:00",
  "endTime": "2026-01-31T23:59:59+07:00",
  "scaleIds": [1, 2, 3],
  "reportCode": "BCSL-001",
  "preparedBy": "Nguyễn Văn A"
}
```

**Response:**
- Content-Type: `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (Word)
- Content-Type: `application/pdf` (PDF)
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (Excel)

---

## 10. Phiên Bản & Lịch Sử

| Phiên Bản | Ngày | Mô Tả |
|-----------|------|-------|
| 1.0 | 2026-01-09 | Phiên bản đầu tiên - Hỗ trợ Word, PDF, Excel template |

---

## 11. Liên Hệ & Hỗ Trợ

Nếu có thắc mắc hoặc cần hỗ trợ thêm về template keys, vui lòng liên hệ team phát triển.

**Generated by:** ScaleHub IoT Development Team  
**Last Updated:** 2026-01-09

