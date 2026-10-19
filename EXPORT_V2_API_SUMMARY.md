# API Export Interval Report V2 Implementation Summary

## Tổng quan

Đã triển khai API **POST `/api/v1/reports/export/v2`** để xuất báo cáo interval V2 với template. API này kết hợp:
- **Dữ liệu**: Từ `/api/v1/reports/interval/v2` (với direction-based overview, start/end values, ratio)
- **Template**: Từ `/api/v1/reports/export` (hỗ trợ Word, PDF, Excel)

## Files Created

### 1. IntervalReportExportRequestV2.java
**Path**: `src/main/java/org/facenet/dto/report/IntervalReportExportRequestV2.java`

**Purpose**: DTO cho request export interval report V2

**Fields**:
- `importId` (Long, required): Template import ID
- `scaleIds` (List<Long>): Danh sách ID cân
- `manufacturerIds` (List<Long>): Danh sách ID nhà sản xuất
- `locationIds` (List<Long>): Danh sách ID địa điểm
- `direction` (String): Hướng cân (IMPORT/EXPORT)
- `fromTime` (OffsetDateTime, required): Thời gian bắt đầu
- `toTime` (OffsetDateTime, required): Thời gian kết thúc
- `interval` (TimeInterval, required): Khoảng thời gian (HOUR, DAY, WEEK, MONTH, YEAR, SHIFT)
- `aggregationByField` (Map): Phương thức tổng hợp cho từng field
- `ratioFormula` (String, default "data_1/data_3"): Công thức tính ratio
- `page`, `size` (Integer): Phân trang

## Files Modified

### 2. EnterpriseReportController.java
**Path**: `src/main/java/org/facenet/controller/report/EnterpriseReportController.java`

**Changes**:
- Added import: `IntervalReportExportRequestV2`
- Added endpoint: `POST /reports/export/v2`
- Added helper method: `determineContentType()` để xác định MIME type

**New Endpoint Details**:
```java
@PostMapping("/reports/export/v2")
public ResponseEntity<byte[]> exportIntervalReportV2(
        @Valid @RequestBody IntervalReportExportRequestV2 request,
        HttpServletRequest httpRequest)
```

**Response Headers**:
- `Content-Disposition`: Filename attachment
- `X-Import-Id`: Template ID used
- `X-Execution-Time-Ms`: Execution duration
- `Content-Type`: Determined from filename (.docx, .pdf, .xlsx, .html)

### 3. ReportExportService.java
**Path**: `src/main/java/org/facenet/service/report/ReportExportService.java`

**New Imports**:
- `PageResponseDto`
- `IntervalReportExportRequestV2`
- `IntervalReportRequestDtoV2`
- `IntervalReportResponseDtoV2`
- `ReportService` (interval report service)
- `OffsetDateTime`
- `Map`, `Collectors`

**New Dependency**:
- `private final ReportService intervalReportService`

**New Methods**:

#### 3.1. exportIntervalReportV2WithTemplate()
Main export method that orchestrates the entire process:
1. Load template from `TemplateImport` record
2. Generate interval report V2 data using `ReportService`
3. Convert `IntervalReportResponseDtoV2` to `ReportData`
4. Route to appropriate export method (Word/PDF/Excel)

#### 3.2. loadTemplateFile()
Load template file from `TemplateImport` with fallback logic:
- Try `resourcePath` first
- Fallback to `filePath` if resourcePath fails

#### 3.3. generateIntervalReportV2Data()
Generate interval report data by calling `intervalReportService.generateIntervalReportV2()`:
- Converts `IntervalReportExportRequestV2` to `IntervalReportRequestDtoV2`
- Sets size=10000 to get all data for export
- Extracts rows from nested PageResponseDto structure

#### 3.4. convertIntervalV2ToReportData()
Convert `IntervalReportResponseDtoV2` structure to `ReportData`:
- Extracts data field names from first row
- Converts V2 rows to ReportData.ReportRow format
- Calculates summary from overview (aggregates across all directions)
- Sets report metadata (title, code, times, prepared by)

#### 3.5. convertV2RowToReportRow()
Convert single `IntervalReportResponseDtoV2.Row` to `ReportData.ReportRow`:
- Extracts scale info (name, ID, location)
- **Parses start values** from `startValues` map (data_1 to data_5)
- **Parses end values** from `endValues` map (data_1 to data_5)
- **Parses aggregated values** from `dataValues` map (data_1 to data_5)
- Sets period, record count, and direction code

#### 3.6. calculateSummaryFromOverview()
Calculate `ReportData.ReportSummary` from direction-based overview:
- Aggregates totals across all directions (0, 1, 2)
- Calculates averages
- Counts total scales and records
- Fallback to `calculateSummaryFromRows()` if overview not available

#### 3.7. calculateDirectionSummaries() **[NEW]**
Calculate direction-specific summaries from overview data:
- Processes each direction in `IntervalReportResponseDtoV2.overview`
- Creates `DirectionSummary` for each direction code (0, 1, 2)
- Extracts stats from overview fieldStats
- Filters rows by direction for accurate counts
- Returns Map<String, DirectionSummary> with keys "0", "1", "2"

#### 3.8. calculateDirectionSummariesFromRows() **[NEW]**
Fallback method when overview not available:
- Groups rows by direction code
- Calculates totals and averages for each group
- Returns same structure as calculateDirectionSummaries()

#### 3.9. getDirectionName() **[NEW]**
Helper to map direction code to name:
- "0" → "Unknown"
- "1" → "Nhập"
- "2" → "Xuất"

#### 3.10. calculateSummaryFromRows()
Fallback method to calculate summary directly from rows:
- Used when overview data is not available
- Sums data_1 to data_5 from all rows
- Calculates averages and counts

#### 3.8. parseDouble()
Helper to parse double values from various types:
- Handles `DataFieldValue` objects
- Handles `Number` types
- Parses strings
- Returns 0.0 for invalid/null values

#### 3.9. generateFilenameForIntervalV2()
Generate export filename:
- Format: `IntervalReportV2_{INTERVAL}_{FROM}_{TO}.{ext}`
- Example: `IntervalReportV2_DAY_20260101_20260113.docx`
- Extension determined from template file

#### 3.10. getCurrentUser()
Get authenticated user name:
- Returns username from SecurityContext
- Fallback to "System" if not authenticated

## Data Flow

```
1. Client Request
   ↓
2. EnterpriseReportController.exportIntervalReportV2()
   ↓
3. ReportExportService.exportIntervalReportV2WithTemplate()
   ↓
4. Load template file from TemplateImport
   ↓
5. Generate interval V2 data via ReportService.generateIntervalReportV2()
   ↓
6. Convert IntervalReportResponseDtoV2 → ReportData
   - convertIntervalV2ToReportData()
   - convertV2RowToReportRow() for each row
   - calculateSummaryFromOverview() for summary
   ↓
7. Route to export method based on template type
   - exportWordTemplate() for .docx
   - exportPdfTemplate() for .html
   - exportExcelTemplate() for .xlsx
   ↓
8. Return file bytes with appropriate headers
```

## ReportData Structure

The converted `ReportData` structure includes:

### Metadata
- `reportTitle`: "Báo cáo khoảng thời gian V2"
- `reportCode`: "INTERVAL_V2"
- `startTime`, `endTime`, `exportTime`
- `preparedBy`: Current authenticated user
- `data1Name` to `data5Name`: Field names from scale config

### Rows
Each `ReportData.ReportRow` contains:
- `rowNumber`: Sequential number
- `scaleId`, `scaleName`, `location`
- `period`: Time period string
- `direction`: Direction code (0=Unknown, 1=Nhập, 2=Xuất)
- **Start values** (from start_values):
  - `data1Start` to `data5Start`: Values at start time
- **End values** (from end_values):
  - `data1End` to `data5End`: Values at end time
- **Aggregated values** (from data_values):
  - `data1Total` to `data5Total`: Aggregated values
- `recordCount`: Number of records in this row

### Summary (Overall)
`ReportData.ReportSummary` contains aggregated statistics across all directions:
- `totalScales`: Unique scale count
- `totalRecords`: Sum of all record counts
- `data1GrandTotal` to `data5GrandTotal`: Totals across all directions
- `data1Average` to `data5Average`: Average values

### Direction Summaries
`ReportData.DirectionSummary` - Map with keys "0", "1", "2":
- `directionCode`: 0, 1, or 2
- `directionName`: "Unknown", "Nhập", or "Xuất"
- `totalScales`: Scale count for this direction
- `totalRecords`: Record count for this direction
- `data1GrandTotal` to `data5GrandTotal`: Totals for this direction
- `data1Average` to `data5Average`: Averages for this direction

## Template Variables

Templates can use these placeholders:

### Organization Info
- `{{organizationName}}`, `{{companyNameEn}}`
- `{{address}}`, `{{phone}}`, `{{email}}`, `{{taxCode}}`
- `{{logoData}}` (binary), `{{hasLogo}}` (boolean)

### Report Info
- `{{reportTitle}}`, `{{reportCode}}`
- `{{startTime}}`, `{{endTime}}`, `{{exportTime}}`
- `{{preparedBy}}`, `{{scaleNames}}`

### Column Names
- `{{data1Name}}` to `{{data5Name}}`

### Data Table (Loop)
```
{{#rows}}
  {{rowNumber}}, {{scaleName}}, {{location}}, {{period}}, {{direction}}
  
  Start values:
  {{data1Start}}, {{data2Start}}, {{data3Start}}, {{data4Start}}, {{data5Start}}
  
  End values:
  {{data1End}}, {{data2End}}, {{data3End}}, {{data4End}}, {{data5End}}
  
  Aggregated values:
  {{data1Total}}, {{data2Total}}, {{data3Total}}, {{data4Total}}, {{data5Total}}
  
  {{recordCount}}
{{/rows}}
```

### Summary Statistics (Overall)
- `{{summary.totalScales}}`, `{{summary.totalRecords}}`
- `{{summary.data1GrandTotal}}` to `{{summary.data5GrandTotal}}`
- `{{summary.data1Average}}` to `{{summary.data5Average}}`

### Direction Summaries (By Direction)

**Cân Unknown (0):**
- `{{directionSummaries.0.directionCode}}`: 0
- `{{directionSummaries.0.directionName}}`: "Unknown"
- `{{directionSummaries.0.totalScales}}`, `{{directionSummaries.0.totalRecords}}`
- `{{directionSummaries.0.data1GrandTotal}}` to `{{directionSummaries.0.data5GrandTotal}}`
- `{{directionSummaries.0.data1Average}}` to `{{directionSummaries.0.data5Average}}`

**Cân Nhập (1):**
- `{{directionSummaries.1.directionCode}}`: 1
- `{{directionSummaries.1.directionName}}`: "Nhập"
- `{{directionSummaries.1.totalScales}}`, `{{directionSummaries.1.totalRecords}}`
- `{{directionSummaries.1.data1GrandTotal}}` to `{{directionSummaries.1.data5GrandTotal}}`
- `{{directionSummaries.1.data1Average}}` to `{{directionSummaries.1.data5Average}}`

**Cân Xuất (2):**
- `{{directionSummaries.2.directionCode}}`: 2
- `{{directionSummaries.2.directionName}}`: "Xuất"
- `{{directionSummaries.2.totalScales}}`, `{{directionSummaries.2.totalRecords}}`
- `{{directionSummaries.2.data1GrandTotal}}` to `{{directionSummaries.2.data5GrandTotal}}`
- `{{directionSummaries.2.data1Average}}` to `{{directionSummaries.2.data5Average}}`

## Key Features

1. **Direction-Based Data**: Automatically aggregates overview from all directions (0, 1, 2)
2. **Start/End/Total Values**: Each data field has 3 values - Start (from start_values), End (from end_values), Total (from data_values)
3. **Direction Summaries**: Separate statistics for Unknown (0), Import/Nhập (1), Export/Xuất (2)
4. **Overall Summary**: Aggregated totals across all directions
5. **Multi-Format Export**: Supports Word, PDF, Excel via template system
6. **Organization Branding**: Logo and company info automatically included
7. **Flexible Filtering**: Supports multiple scales, manufacturers, locations, direction
8. **Time Aggregation**: Supports all interval types (HOUR, DAY, WEEK, MONTH, YEAR, SHIFT)

## Testing Notes

- Ensure `importId` exists in `template_imports` table
- Template file must exist at `resourcePath` or `filePath`
- Template type detected from filename extension (.docx, .html, .xlsx)
- All data from interval/v2 will be included (size=10000)
- Summary aggregates across all directions

## Future Enhancements

Possible extensions:
1. ~~Map `startValues` and `endValues` to ReportData for template access~~ ✅ **COMPLETED**
2. Add `ratio` field to ReportData.ReportRow (currently available in V2 but not mapped)
3. ~~Support direction-specific summaries in template~~ ✅ **COMPLETED**
4. Add configurable field visibility (hide unused data fields)
5. Support custom summary calculations per template
6. Add direction filtering in template (show only Import or Export rows)

## Build Status

✅ **BUILD SUCCESS** - All changes compiled successfully.
