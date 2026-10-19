# Hướng Dẫn Tạo Template Word - Phương Pháp Insert Trực Tiếp (v2)

## ✅ Phương pháp mới - Tự động đọc placeholder

Hệ thống sử dụng **phương pháp insert trực tiếp** vào bảng Word và **TỰ ĐỘNG ĐỌC placeholder** từ dòng template để map dữ liệu chính xác.

## 📝 Cách tạo template

### Bước 1: Tạo phần header (ngoài bảng)

Sử dụng các placeholder đơn giản:

```
CÔNG TY {{organizationName}}
Địa chỉ: {{address}}
Điện thoại: {{phone}} | Email: {{email}}

========================================

{{reportTitle}}
Mã báo cáo: {{reportCode}}

Từ ngày: {{startTime}} đến {{endTime}}
Trạm cân: {{scaleNames}}
Người lập: {{preparedBy}}
Thời gian xuất: {{exportTime}}
```

### Bước 2: Tạo bảng dữ liệu (QUAN TRỌNG!)

**Bảng CẦN 2 DÒNG:**

1. **Dòng 1 (Header):** Tiêu đề cột (có thể tùy chỉnh)
2. **Dòng 2 (Template):** Chứa **placeholder** `{{key}}` để hệ thống biết map data

**Ví dụ template với 5 cột:**

```
+-----+----------+---------------+---------------+---------------+
| STT | Mã cân   | Tên cân       | Data 1        | Data 2        |
+-----+----------+---------------+---------------+---------------+
| {{rowNumber}} | {{scaleCode}} | {{scaleName}} | {{data1Total}} | {{data2Total}} |
+-----+----------+---------------+---------------+---------------+
```

**✅ Cách hoạt động:**
1. Hệ thống đọc dòng 2 (template row)
2. Tìm placeholder trong mỗi ô: `{{rowNumber}}`, `{{scaleCode}}`, ...
3. Map dữ liệu vào đúng cột dựa trên placeholder tìm được
4. Copy dòng template và điền dữ liệu cho mỗi row
5. Xóa dòng template gốc

**🎯 Ưu điểm:**
- ✅ Linh hoạt: Bạn quyết định cột nào chứa data gì
- ✅ Tự động: Không cần sửa code khi thay đổi thứ tự cột
- ✅ Rõ ràng: Nhìn vào template là biết cấu trúc

### Bước 3: Thêm phần tổng kết (ngoài bảng)

```
========================================
TỔNG KẾT:
- Tổng số trạm: {{summary.totalScales}}
- Tổng số bản ghi: {{summary.totalRecords}}
- Tổng Data 1: {{summary.data1GrandTotal}}
- Tổng Data 2: {{summary.data2GrandTotal}}
- Trung bình Data 1: {{summary.data1Average}}
```

## 🎯 Các placeholder có sẵn cho bảng

| Placeholder       | Mô tả                          | Format              |
|-------------------|--------------------------------|---------------------|
| {{rowNumber}}     | Số thứ tự                      | 1, 2, 3, ...        |
| {{scaleCode}}     | Mã trạm cân                    | TC001, TC002, ...   |
| {{scaleName}}     | Tên trạm cân                   | Trạm cân 1, ...     |
| {{location}}      | Vị trí                         | Kho A, Nhà máy B    |
| {{data1Total}}    | Tổng Data 1                    | 1.234,56            |
| {{data2Total}}    | Tổng Data 2                    | 2.345,67            |
| {{data3Total}}    | Tổng Data 3                    | 3.456,78            |
| {{data4Total}}    | Tổng Data 4                    | 4.567,89            |
| {{data5Total}}    | Tổng Data 5                    | 5.678,90            |
| {{recordCount}}   | Số lượng bản ghi               | 100                 |
| {{period}}        | Chu kỳ/thời điểm               | 2025-01-01, Ca 1    |
| {{lastTime}}      | Thời gian cuối                 | 06/01/2025 10:30:00 |

**Lưu ý:** Bạn chỉ cần dùng các placeholder bạn cần. Không bắt buộc phải có tất cả.

## 📋 Ví dụ template đầy đủ

### Template 1: Báo cáo đơn giản (3 cột)

```
                    CÔNG TY {{organizationName}}
                Địa chỉ: {{address}}
                ĐT: {{phone}} | Email: {{email}}
                Mã số thuế: {{taxCode}}

================================================================================

                    {{reportTitle}}
                    Mã số: {{reportCode}}
                    
Từ ngày: {{startTime}}              Đến ngày: {{endTime}}
Trạm cân: {{scaleNames}}
Người lập: {{preparedBy}}
Thời gian xuất: {{exportTime}}

--------------------------------------------------------------------------------

BẢNG DỮ LIỆU:

+-------+----------+------------------+------------------+------------------+
| STT   | Mã cân   | Tên trạm         | {{data1Name}}    | {{data2Name}}    |
+-------+----------+------------------+------------------+------------------+
| 1     | TC001    | Trạm cân số 1    | 1.000,00         | 2.000,00         |
+-------+----------+------------------+------------------+------------------+

Dòng thứ 2 của bảng (dòng có số "1") sẽ bị xóa và thay thế bằng dữ liệu thực.

--------------------------------------------------------------------------------

TỔNG KẾT:
- Tổng số trạm cân: {{summary.totalScales}}
- Tổng số bản ghi: {{summary.totalRecords}}
- Tổng {{data1Name}}: {{summary.data1GrandTotal}}
- Tổng {{data2Name}}: {{summary.data2GrandTotal}}
- Trung bình {{data1Name}}: {{summary.data1Average}}
- Trung bình {{data2Name}}: {{summary.data2Average}}
- Giá trị lớn nhất {{data1Name}}: {{summary.data1Max}}
- Giá trị lớn nhất {{data2Name}}: {{summary.data2Max}}

Người lập: {{preparedBy}}
Ngày lập: {{currentDateTime}}
```

## 🔧 Tùy chỉnh số cột

Nếu bạn muốn thay đổi thứ tự hoặc số cột, sửa trong code:

[ReportExportService.java](d:/ScaleHubIOT/src/main/java/org/facenet/service/report/ReportExportService.java) → `fillRowWithData()`:

```java
String[] columnKeys = {
    "rowNumber",    // Cột 1
    "scaleCode",    // Cột 2
    "scaleName",    // Cột 3
    "data1Total",   // Cột 4
    "data2Total",   // Cột 5
    // ... thêm hoặc bớt cột
};
```

## ✅ Ưu điểm phương pháp mới

1. ✅ **Đơn giản hơn** - Không cần hiểu POI-TL loop syntax
2. ✅ **Không bị lỗi tag** - Không có `{{#rows}}` hay `{{/rows}}`
3. ✅ **Linh hoạt** - Dễ customize số cột và thứ tự
4. ✅ **Ổn định** - Không phụ thuộc vào Word XML structure

## 🎨 Styling

Dòng template (dòng 2 của bảng) sẽ được copy về styling:
- Font, size, color
- Border, alignment
- Background color
- Padding

→ Hãy format dòng template đẹp như bạn muốn!

## 🚀 Test

1. Tạo template Word theo hướng dẫn trên
2. Upload vào hệ thống
3. Export báo cáo
4. Kiểm tra file output

Nếu có vấn đề, kiểm tra log:
```
Inserting X rows into table
Found table with Y rows
Template row has Z columns
Successfully inserted X data rows and removed template row
```

## 📝 Checklist

- [ ] Template có phần header với các placeholder
- [ ] Bảng có đúng 2 dòng (header + template)
- [ ] Dòng template được format đẹp
- [ ] Có phần tổng kết với {{summary.*}}
- [ ] Không có tag {{#rows}} hay {{/rows}} trong template
- [ ] File lưu dạng .docx (Office Open XML)
