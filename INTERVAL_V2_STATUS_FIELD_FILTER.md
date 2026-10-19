# Interval V2 API - Status Field Filter

## Mô tả thay đổi

API `/api/v1/reports/interval/v2` đã được cập nhật để **tự động lọc bỏ** các trường dữ liệu (`data_n`) có tên là **"Trạng thái"** khỏi response.

## Chi tiết kỹ thuật

### 1. Các thay đổi trong code

**File**: `ReportServiceImpl.java`

#### Method mới được thêm vào:

1. **`filterOutStatusFields()`** - Lọc bỏ các data field có name = "Trạng thái"
   ```java
   private Map<String, IntervalReportResponseDtoV2.DataFieldInfo> filterOutStatusFields(
           Map<String, IntervalReportResponseDtoV2.DataFieldInfo> dataFieldNames)
   ```
   - Duyệt qua tất cả data field names
   - Loại bỏ các field có `name` = "Trạng thái"
   - Trả về map đã được lọc

2. **`filterDataValuesByFields()`** - Lọc data values map theo danh sách fields hợp lệ
   ```java
   private Map<String, IntervalReportResponseDtoV2.DataFieldValue> filterDataValuesByFields(
           Map<String, IntervalReportResponseDtoV2.DataFieldValue> dataValues,
           Map<String, IntervalReportResponseDtoV2.DataFieldInfo> dataFieldNames)
   ```
   - Chỉ giữ lại các key có trong `dataFieldNames`
   - Đảm bảo consistency giữa field names và actual values

#### Thay đổi trong method `generateIntervalReportV2()`:

```java
// Get data field names with units first
Map<String, IntervalReportResponseDtoV2.DataFieldInfo> dataFieldNames = extractDataFieldNamesWithUnits(scaleIds);

// Filter out fields with name "Trạng thái"
dataFieldNames = filterOutStatusFields(dataFieldNames);

// ... trong vòng lặp xử lý rows:

// Filter out status fields from all value maps
startValues = filterDataValuesByFields(startValues, dataFieldNames);
endValues = filterDataValuesByFields(endValues, dataFieldNames);
dataValues = filterDataValuesByFields(dataValues, dataFieldNames);
```

### 2. Tác động

#### Các phần bị ảnh hưởng:

1. **Response JSON** - Không còn chứa data_n với name "Trạng thái"
   - `dataFieldNames` object
   - `start_values` trong mỗi row
   - `end_values` trong mỗi row
   - `data_values` trong mỗi row
   - `overview` statistics
   - `dataFieldSummaries`

2. **Export template** - Tự động áp dụng filter
   - API `/api/v1/reports/enterprise/interval/v2/export` 
   - Vì gọi `generateIntervalReportV2()` nên sẽ có cùng filter

### 3. Ví dụ

#### Trước khi filter:

Scale config có:
- `data_1`: "Weight"
- `data_2`: "Trạng thái"
- `data_3`: "Temperature"

Response sẽ chứa cả 3 fields.

#### Sau khi filter:

Response chỉ chứa:
- `data_1`: "Weight"
- `data_3`: "Temperature"

Field `data_2` với name "Trạng thái" đã bị loại bỏ hoàn toàn.

### 4. Lý do thay đổi

- Trường "Trạng thái" không cần thiết trong báo cáo thống kê interval
- Giảm dung lượng response
- Tối ưu performance
- Làm sạch dữ liệu không cần thiết

## Backward Compatibility

✅ **Hoàn toàn tương thích ngược**

- Client code hiện tại sẽ không bị lỗi
- Response structure không thay đổi, chỉ bớt đi một số fields
- Không ảnh hưởng đến logic xử lý hiện tại

## Testing

Để test thay đổi:

1. Tạo scale config với một data field có name = "Trạng thái"
2. Gọi API `/api/v1/reports/interval/v2`
3. Verify rằng response không chứa field đó trong:
   - `dataFieldNames`
   - `start_values`, `end_values`, `data_values` của các rows
   - `overview`
   - `dataFieldSummaries`

## Ngày thay đổi

2026-01-16
