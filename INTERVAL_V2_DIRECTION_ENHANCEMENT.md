# API /reports/interval/v2 - Direction-Based Overview Enhancement

## Tổng quan

API `/api/v1/reports/interval/v2` đã được cải tiến để hỗ trợ phân tách thống kê theo **hướng của cân** (direction):
- **0**: Unknown (không xác định)
- **1**: IMPORT (nhập)
- **2**: EXPORT (xuất)

## Thay đổi trong Response Structure

### 1. Row Object - Thêm field `direction`

Mỗi bản ghi row giờ có thêm trường `direction` với giá trị integer (0, 1, hoặc 2):

```json
{
  "scale": {...},
  "period": "2024-01-01 00:00",
  "direction": 1,  // <-- FIELD MỚI: 0=unknown, 1=import, 2=export
  "recordCount": 150,
  "startValues": {...},
  "endValues": {...},
  "dataValues": {...},
  "ratio": {...}
}
```

### 2. ScaleInfo Object - Thêm field `direction`

Thông tin scale trong mỗi row giờ có thêm trường `direction` dạng string:

```json
{
  "id": 1,
  "name": "Cân A",
  "model": "XYZ-100",
  "type": "Digital",
  "isActive": true,
  "location": {...},
  "direction": "IMPORT",  // <-- FIELD MỚI: "IMPORT" hoặc "EXPORT"
  "createdAt": "2024-01-01T10:00:00Z",
  ...
}
```

### 3. Overview Structure - Grouped by Direction

Cấu trúc `overview` đã thay đổi từ flat structure sang nested structure theo direction:

**Trước đây (V1):**
```json
{
  "overview": {
    "data_1": {
      "value": "1000.00",
      "aggregation": "SUM",
      "name": "Khối lượng",
      "used": true
    },
    "data_2": {...}
  }
}
```

**Bây giờ (V2 - Grouped by Direction):**
```json
{
  "overview": {
    "1": {  // IMPORT scales
      "data_1": {
        "value": "600.00",
        "aggregation": "SUM",
        "name": "Khối lượng",
        "used": true
      },
      "data_2": {...}
    },
    "2": {  // EXPORT scales
      "data_1": {
        "value": "400.00",
        "aggregation": "SUM",
        "name": "Khối lượng",
        "used": true
      },
      "data_2": {...}
    },
    "0": {  // Unknown direction scales (if any)
      "data_1": {...}
    }
  }
}
```

## Logic Implementation

### 1. Direction Code Mapping

Method `directionToCode()` trong `ReportServiceImpl`:
- `null` hoặc blank → `0` (unknown)
- `"IMPORT"` → `1` (nhập)
- `"EXPORT"` → `2` (xuất)
- Các giá trị khác → `0` (unknown)

### 2. ScaleInfo Conversion

Method `convertScaleInfo()`:
- Fetch `Scale` entity từ database qua `scaleRepository`
- Lấy `direction` enum từ entity
- Convert sang string (`IMPORT` hoặc `EXPORT`)
- Gán vào `ScaleInfo.direction`

### 3. Row Building

Khi build `IntervalReportResponseDtoV2.Row`:
- Gọi `convertScaleInfo()` để có đầy đủ thông tin scale bao gồm direction
- Gọi `directionToCode()` để convert direction string sang integer code
- Gán vào `Row.direction`

### 4. Overview Calculation

Method `calculateOverview()`:
1. **Group rows by direction**: Sử dụng Java Stream `groupingBy()` để nhóm rows theo `Row.direction`
2. **Calculate per direction**: 
   - Với mỗi nhóm direction (0, 1, 2), tính toán riêng biệt:
     - `data_1`: SUM
     - `data_2` đến `data_5`: AVG
3. **Build nested structure**: 
   - Key ngoài: direction code (`"0"`, `"1"`, `"2"`)
   - Key trong: data field (`"data_1"`, `"data_2"`, ...)
   - Value: `OverviewStats` với value, aggregation, name, used

## Example Response

```json
{
  "data": [
    {
      "interval": "HOUR",
      "fromDate": "2024-01-01T00:00:00Z",
      "toDate": "2024-01-01T23:59:59Z",
      "dataFieldNames": {...},
      "aggregationByField": {...},
      "ratioFormula": "data_1/data_3",
      "overview": {
        "1": {
          "data_1": {
            "value": "600.50",
            "aggregation": "SUM",
            "name": "Khối lượng",
            "used": true
          },
          "data_2": {
            "value": "25.30",
            "aggregation": "AVG",
            "name": "Nhiệt độ",
            "used": true
          }
        },
        "2": {
          "data_1": {
            "value": "399.50",
            "aggregation": "SUM",
            "name": "Khối lượng",
            "used": true
          },
          "data_2": {
            "value": "24.80",
            "aggregation": "AVG",
            "name": "Nhiệt độ",
            "used": true
          }
        }
      },
      "rows": [
        {
          "scale": {
            "id": 1,
            "name": "Cân nhập A",
            "direction": "IMPORT",
            ...
          },
          "period": "2024-01-01 00:00",
          "direction": 1,
          "recordCount": 50,
          "startValues": {...},
          "endValues": {...},
          "dataValues": {...},
          "ratio": {...}
        },
        {
          "scale": {
            "id": 2,
            "name": "Cân xuất B",
            "direction": "EXPORT",
            ...
          },
          "period": "2024-01-01 00:00",
          "direction": 2,
          "recordCount": 40,
          "startValues": {...},
          "endValues": {...},
          "dataValues": {...},
          "ratio": {...}
        }
      ]
    }
  ],
  "page": 0,
  "size": 1,
  ...
}
```

## Files Modified

1. **IntervalReportResponseDtoV2.java**:
   - Added `direction` field to `Row` class (Integer)
   - Added `direction` field to `ScaleInfo` class (String)
   - Changed `overview` type from `Map<String, OverviewStats>` to `Map<String, Map<String, OverviewStats>>`

2. **ReportServiceImpl.java**:
   - Added import: `org.facenet.entity.scale.Scale`
   - Added import: `org.facenet.repository.scale.ScaleRepository`
   - Added field: `private final ScaleRepository scaleRepository`
   - Modified `convertScaleInfo()`: Fetch direction from Scale entity
   - Modified row building in `generateIntervalReportV2()`: Set direction code
   - Modified `calculateOverview()`: Group by direction and calculate nested statistics
   - Added new method `directionToCode()`: Convert direction string to integer code

## Testing Notes

- Scales có `direction = null` sẽ được gán code `0` (unknown)
- Overview sẽ chỉ chứa các direction có data (nếu không có scale nào có direction=0, key "0" sẽ không xuất hiện)
- Mỗi direction group có thống kê độc lập cho từng data field
- Aggregation rules vẫn giữ nguyên: data_1=SUM, data_2-5=AVG

## Build Status

✅ **BUILD SUCCESS** - Compilation completed without errors.
