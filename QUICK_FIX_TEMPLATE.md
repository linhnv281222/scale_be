# QUICK FIX - Template Word Đơn Giản

## Cách 1: Sử dụng bảng 2 dòng với inline loop

Tạo bảng Word như sau (CHỈ 2 DÒNG):

```
┌────────────────────────┬──────────────────────────┬──────────────────────┐
│ STT                    │ Mã cân                   │ Tên cân              │
├────────────────────────┼──────────────────────────┼──────────────────────┤
│{{#rows}}{{rowNumber}}{{/rows}}│{{#rows}}{{scaleCode}}{{/rows}}│{{#rows}}{{scaleName}}{{/rows}}│
└────────────────────────┴──────────────────────────┴──────────────────────┘
```

**Lưu ý:** Mỗi ô dữ liệu đều có `{{#rows}}...{{/rows}}` bao quanh!

## Cách 2: Sử dụng bảng 4 dòng với LoopRowTableRenderPolicy

```
┌────────────────────────┬──────────────────────────┬──────────────────────┐
│ STT                    │ Mã cân                   │ Tên cân              │
├────────────────────────┼──────────────────────────┼──────────────────────┤
│ {{#rows}}              │                          │                      │
├────────────────────────┼──────────────────────────┼──────────────────────┤
│ {{rowNumber}}          │ {{scaleCode}}            │ {{scaleName}}        │
├────────────────────────┼──────────────────────────┼──────────────────────┤
│ {{/rows}}              │                          │                      │
└────────────────────────┴──────────────────────────┴──────────────────────┘
```

**Quan trọng:**
- Dòng 1: Header
- Dòng 2: {{#rows}} ở ô đầu tiên, các ô khác để trống
- Dòng 3: CÓ placeholder CHO TẤT CẢ các cột
- Dòng 4: {{/rows}} ở ô đầu tiên, các ô khác để trống

## Cách 3: Không dùng bảng (ĐẢM BẢO WORK)

Nếu 2 cách trên vẫn lỗi, xóa bảng đi và dùng:

```
DANH SÁCH DỮ LIỆU:

{{#rows}}
STT: {{rowNumber}}
Mã cân: {{scaleCode}}
Tên cân: {{scaleName}}
Data 1: {{data1Total}}
Data 2: {{data2Total}}
-----------------------------------
{{/rows}}

Tổng: {{summary.totalRecords}} bản ghi
```

Cách này chắc chắn sẽ work!

## Template mẫu đầy đủ (không dùng bảng)

```
                CÔNG TY {{organizationName}}
            Địa chỉ: {{address}} | ĐT: {{phone}}
            
========================================================

            {{reportTitle}}
            Mã: {{reportCode}}
            
Từ ngày: {{startTime}} đến {{endTime}}
Người lập: {{preparedBy}}
Thời gian xuất: {{exportTime}}

========================================================

DANH SÁCH CHI TIẾT:

{{#rows}}
[{{rowNumber}}] Trạm: {{scaleName}} ({{scaleCode}})
    - Vị trí: {{location}}
    - {{data1Name}}: {{data1Total}}
    - {{data2Name}}: {{data2Total}}
    - {{data3Name}}: {{data3Total}}
    - Số bản ghi: {{recordCount}}
    - Thời gian cuối: {{lastTime}}
========================================================
{{/rows}}

TỔNG KẾT:
- Tổng số trạm cân: {{summary.totalScales}}
- Tổng số bản ghi: {{summary.totalRecords}}
- Tổng {{data1Name}}: {{summary.data1GrandTotal}}
- Tổng {{data2Name}}: {{summary.data2GrandTotal}}
- Tổng {{data3Name}}: {{summary.data3GrandTotal}}

Người lập báo cáo
{{preparedBy}}
```

Copy template này vào Word và test - ĐẢM BẢO sẽ work!
