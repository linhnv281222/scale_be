# Report Template Guide

Since Word templates are binary files (.docx), you need to create the template manually using Microsoft Word.

## How to Create the Word Template (report-template.docx)

1. **Open Microsoft Word** and create a new document

2. **Add Header Section:**
   ```
   {{reportTitle}}
   
   Mã báo cáo: {{reportCode}}
   Thời gian: {{dateRange}}
   Thời gian xuất: {{exportTime}}
   Người thực hiện: {{preparedBy}}
   ```

3. **Add Data Table:**
   Create a table with the following headers:
   - STT | Mã trạm | Tên trạm | Vị trí | Data 1 | Data 2 | Data 3 | Data 4 | Data 5 | Số lần cân | Thời gian cuối

4. **Add a data row with placeholders:**
   ```
   {{#rows}}
   {{rowNumber}} | {{scaleCode}} | {{scaleName}} | {{location}} | {{data1Total}} | {{data2Total}} | {{data3Total}} | {{data4Total}} | {{data5Total}} | {{recordCount}} | {{lastTime}}
   {{/rows}}
   ```

5. **Add Summary Section:**
   ```
   TỔNG HỢP
   
   Tổng số trạm: {{totalScales}}
   Tổng số bản ghi: {{totalRecords}}
   
   Tổng Data 1: {{data1GrandTotal}} kg
   Tổng Data 2: {{data2GrandTotal}} kg
   Tổng Data 3: {{data3GrandTotal}} kg
   Tổng Data 4: {{data4GrandTotal}} kg
   Tổng Data 5: {{data5GrandTotal}} kg
   
   Trung bình Data 1: {{data1Average}} kg
   ```

6. **Add Chart Placeholder:**
   ```
   {{@chart}}
   ```

7. **Add Signature Section:**
   ```
   Người lập biểu          Trưởng bộ phận          Giám đốc
   ```

8. **Save the file** as `report-template.docx` in:
   ```
   src/main/resources/templates/report-template.docx
   ```

## Template Variables Reference

### Header Variables:
- `{{reportTitle}}` - Report title
- `{{reportCode}}` - Report code
- `{{dateRange}}` - Date range string
- `{{exportTime}}` - Export timestamp
- `{{preparedBy}}` - Person who prepared the report

### Loop Variables (in {{#rows}} ... {{/rows}}):
- `{{rowNumber}}` - Row number
- `{{scaleCode}}` - Scale code
- `{{scaleName}}` - Scale name
- `{{location}}` - Location name
- `{{data1Total}}` - Data 1 total (formatted)
- `{{data2Total}}` - Data 2 total (formatted)
- `{{data3Total}}` - Data 3 total (formatted)
- `{{data4Total}}` - Data 4 total (formatted)
- `{{data5Total}}` - Data 5 total (formatted)
- `{{recordCount}}` - Number of records
- `{{lastTime}}` - Last measurement time

### Summary Variables:
- `{{totalScales}}` - Total number of scales
- `{{totalRecords}}` - Total number of records
- `{{data1GrandTotal}}` - Grand total for Data 1
- `{{data2GrandTotal}}` - Grand total for Data 2
- `{{data3GrandTotal}}` - Grand total for Data 3
- `{{data4GrandTotal}}` - Grand total for Data 4
- `{{data5GrandTotal}}` - Grand total for Data 5
- `{{data1Average}}` - Average for Data 1
- `{{data2Average}}` - Average for Data 2
- etc.

### Special Variables:
- `{{@chart}}` - Chart image (automatically generated)

## Styling Tips:

1. Use **Times New Roman, size 13** for professional administrative documents
2. Make headers **bold and centered**
3. Use table borders for data tables
4. Add company logo if needed
5. Use page numbers in footer

## POI-TL Syntax Reference:

- `{{variable}}` - Simple text replacement
- `{{#list}}...{{/list}}` - Loop through list
- `{{@image}}` - Insert image
- `{{?condition}}...{{/condition}}` - Conditional rendering
