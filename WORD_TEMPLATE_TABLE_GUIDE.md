# Hướng Dẫn Tạo Template Word Với Bảng Dữ Liệu (POI-TL)

## Vấn đề: Bảng không có dữ liệu khi xuất

Nếu file Word xuất ra không có dữ liệu trong bảng, có thể do:
1. **Template Word không được cấu hình đúng cú pháp POI-TL**
2. **Vị trí đặt placeholder không đúng trong table**
3. **Thiếu cấu hình LoopRowTableRenderPolicy**

## Cách tạo Template đúng

### Option 1: Sử dụng LoopRowTableRenderPolicy (Cho bảng Word)

**Bước 1: Tạo bảng trong Word**

```
+------------+-------------+-------------+--------------+--------------+
| STT        | Mã cân      | Tên cân     | Data 1       | Data 2       |
+------------+-------------+-------------+--------------+--------------+
| {{#rows}}  |             |             |              |              |
+------------+-------------+-------------+--------------+--------------+
| {{rowNumber}} | {{scaleCode}} | {{scaleName}} | {{data1Total}} | {{data2Total}} |
+------------+-------------+-------------+--------------+--------------+
| {{/rows}}  |             |             |              |              |
+------------+-------------+-------------+--------------+--------------+
```

**QUAN TRỌNG:**
- Dòng đầu tiên của bảng (header) chứa tiêu đề cột
- Dòng thứ hai chứa `{{#rows}}` ở ô đầu tiên - đây là dòng template sẽ bị xóa
- Dòng thứ ba chứa các placeholder dữ liệu ({{rowNumber}}, {{scaleCode}}, ...)
- Dòng thứ tư chứa `{{/rows}}` ở ô đầu tiên - đây là dòng template sẽ bị xóa
- POI-TL sẽ lặp dòng thứ ba (dòng chứa data) cho mỗi item trong danh sách

**Cấu hình Java:**
```java
LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();
Configure config = Configure.builder()
        .bind("rows", policy)
        .build();
```

### Option 2: Sử dụng Loop đơn giản (Không phải bảng)

Nếu bạn không muốn dùng table của Word, có thể dùng loop đơn giản:

```
{{#rows}}
- STT: {{rowNumber}}
- Mã cân: {{scaleCode}}
- Tên cân: {{scaleName}}
- Data 1: {{data1Total}}
- Data 2: {{data2Total}}
---
{{/rows}}
```

**Cấu hình Java:**
```java
Configure config = Configure.builder().build(); // Không cần policy
```

### Option 3: Sử dụng cú pháp [# ] cho bảng (Cách khác)

Một số version POI-TL hỗ trợ cú pháp:

```
+------------+-------------+-------------+--------------+
| STT        | Mã cân      | Tên cân     | Data 1       |
+------------+-------------+-------------+--------------+
| [#rows]{{rowNumber}}[/] | [#rows]{{scaleCode}}[/] | [#rows]{{scaleName}}[/] | [#rows]{{data1Total}}[/] |
+------------+-------------+-------------+--------------+
```

## Template Variables Có Sẵn

### Thông tin header (ngoài bảng):
```
{{reportTitle}}        - Tiêu đề báo cáo
{{reportCode}}         - Mã báo cáo  
{{exportTime}}         - Thời gian xuất (dd/MM/yyyy HH:mm:ss)
{{startTime}}          - Ngày bắt đầu (dd/MM/yyyy)
{{endTime}}            - Ngày kết thúc (dd/MM/yyyy)
{{preparedBy}}         - Người xuất báo cáo
{{scaleNames}}         - Danh sách trạm cân
{{organizationName}}   - Tên công ty
{{address}}            - Địa chỉ
{{phone}}              - Số điện thoại
{{email}}              - Email
```

### Tên cột động (từ cấu hình scale):
```
{{data1Name}}          - Tên cột Data 1
{{data2Name}}          - Tên cột Data 2
{{data3Name}}          - Tên cột Data 3
{{data4Name}}          - Tên cột Data 4
{{data5Name}}          - Tên cột Data 5
```

### Dữ liệu từng dòng (trong loop rows):
```
{{rowNumber}}          - Số thứ tự
{{scaleId}}            - ID trạm cân
{{scaleCode}}          - Mã trạm cân
{{scaleName}}          - Tên trạm cân
{{location}}           - Vị trí
{{period}}             - Thời điểm/chu kỳ
{{data1Total}}         - Tổng Data 1 (đã format: 1.234,56)
{{data2Total}}         - Tổng Data 2 (đã format)
{{data3Total}}         - Tổng Data 3 (đã format)
{{data4Total}}         - Tổng Data 4 (đã format)
{{data5Total}}         - Tổng Data 5 (đã format)
{{recordCount}}        - Số lượng bản ghi
{{lastTime}}           - Thời gian cuối (dd/MM/yyyy HH:mm:ss)
```

### Tổng kết (ngoài bảng hoặc ở cuối bảng):
```
{{summary.totalScales}}       - Tổng số trạm cân
{{summary.totalRecords}}      - Tổng số bản ghi
{{summary.data1GrandTotal}}   - Tổng lớn Data 1 (đã format)
{{summary.data2GrandTotal}}   - Tổng lớn Data 2 (đã format)
{{summary.data3GrandTotal}}   - Tổng lớn Data 3 (đã format)
{{summary.data4GrandTotal}}   - Tổng lớn Data 4 (đã format)
{{summary.data5GrandTotal}}   - Tổng lớn Data 5 (đã format)
{{summary.data1Average}}      - Trung bình Data 1
{{summary.data2Average}}      - Trung bình Data 2
...
```

## Ví dụ Template Hoàn Chỉnh

```
                        CÔNG TY {{organizationName}}
                    Địa chỉ: {{address}}
                    ĐT: {{phone}} | Email: {{email}}

================================================================================

                    {{reportTitle}}
                    Mã số: {{reportCode}}
                    
Từ ngày: {{startTime}}              Đến ngày: {{endTime}}
Trạm cân: {{scaleNames}}
Thời gian xuất: {{exportTime}}

+-------+----------+------------------+------------------+------------------+
| STT   | Mã cân   | {{data1Name}}    | {{data2Name}}    | {{data3Name}}    |
+-------+----------+------------------+------------------+------------------+
|{{#rows}}|        |                  |                  |                  |
+-------+----------+------------------+------------------+------------------+
|{{rowNumber}}|{{scaleCode}}|{{data1Total}}|{{data2Total}}|{{data3Total}}|
+-------+----------+------------------+------------------+------------------+
|{{/rows}}|       |                  |                  |                  |
+-------+----------+------------------+------------------+------------------+

TỔNG KẾT:
- Tổng số trạm: {{summary.totalScales}}
- Tổng số bản ghi: {{summary.totalRecords}}
- Tổng {{data1Name}}: {{summary.data1GrandTotal}}
- Tổng {{data2Name}}: {{summary.data2GrandTotal}}

Người lập: {{preparedBy}}
```

## Cách Debug

### 1. Kiểm tra log khi xuất báo cáo:

Tìm trong log các dòng sau:
```
Queried data: X rows
Data model prepared - rows count: X
First row sample: {rowNumber=1, scaleCode=..., data1Total=...}
```

Nếu thấy "rows count: 0" hoặc "NOT A LIST" → Vấn đề ở query data
Nếu thấy có data nhưng file Word vẫn trống → Vấn đề ở template

### 2. Test với template đơn giản:

Tạo file Word chỉ với nội dung:
```
Số dòng: {{totalLogs}}

{{#rows}}
{{rowNumber}}. {{scaleCode}} - {{scaleName}}: {{data1Total}}
{{/rows}}
```

Nếu cách này work → Vấn đề là cấu trúc bảng trong template gốc

### 3. Kiểm tra encoding của file Word:

- File .docx phải là định dạng Office Open XML chuẩn
- Không được chứa ký tự đặc biệt trong placeholder
- Copy-paste từ source khác có thể tạo invisible characters

## Giải pháp nhanh

**Nếu bạn gặp vấn đề:**

1. **Thử không dùng table policy:**
```java
Configure config = Configure.builder().build();
```

Và dùng cú pháp loop đơn giản trong template (không dùng bảng Word).

2. **Hoặc thử cú pháp table đơn giản hơn:**

Chỉ có 2 dòng trong table:
- Dòng 1: Header (STT | Mã cân | Tên cân | ...)
- Dòng 2: {{#rows}}{{rowNumber}}{{/rows}} | {{#rows}}{{scaleCode}}{{/rows}} | ...

## Tham khảo

- POI-TL Documentation: https://deepoove.com/poi-tl/
- POI-TL Table Loop: https://deepoove.com/poi-tl/#_%E5%BE%AA%E7%8E%AF%E8%A1%A8%E6%A0%BC%E8%A1%8C
