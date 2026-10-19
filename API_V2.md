## GET http://localhost:8080/api/v1/scales/v2

* Parameters:
- `page`: integer
- `size`: integer
- `sort`: string ("field,asc/desc")
- `search`: string
-  `locationId`: array[integer]
- `manufacturerId`: array[integer]
- `protocolId`: array[integer]
- `model`: string
- `direction`: string - loại cân Nhập/Xuất
- `isActive`: boolean

* Response code: 200
* Response body: 
``` json
{
  "success": true,
  "data": {
    "data": {
      "data": [
        {
          "id": 1,
          "name": "Cân 11",
          "model": "IND570",
          "location_id": 1,
          "location_name": "string",
          "is_active": true,
          "created_at": "2025-12-24T07:11:45.002195Z",
          "created_by": "admin",
          "updated_at": "2025-12-25T07:48:44.222954Z",
          "updated_by": "admin",
          "scale_config": {
            "protocol": "MODBUS_TCP",
            "scale_id": 1,
            "poll_interval": 1000,
            "conn_params": {
              "ip": "192.168.1.10",
              "port": 502
            },
            "data_1": {
              "name": "Weight",
              "is_used": true,
              "num_registers": 2,
              "start_registers": 40001
            },
            "data_2": {
              "is_used": false
            },
            "data_3": {
              "name": "WeightJHGSJKF",
              "is_used": true,
              "data_type": "integer",
              "num_registers": 2,
              "start_registers": 40001
            },
            "data_4": {
              "is_used": false
            },
            "data_5": {
              "is_used": false
            }
          }
        },
        {
          "id": 2,
          "name": "Cân 12",
          "model": "NMH002",
          "location_id": 1,
          "location_name": "string",
          "is_active": true,
          "created_at": "2025-12-25T08:13:43.377903Z",
          "created_by": "admin",
          "updated_at": "2025-12-25T08:13:43.377903Z",
          "updated_by": "admin",
          "scale_config": {
            "protocol": "MODBUS_TCP",
            "scale_id": 2,
            "poll_interval": 1000,
            "conn_params": {
              "ip": "192.168.1.10",
              "port": 502
            },
            "data_1": {
              "name": "Weight",
              "is_used": true,
              "num_registers": 2,
              "start_registers": 40001
            },
            "data_2": {
              "is_used": false
            },
            "data_3": {
              "is_used": false
            },
            "data_4": {
              "is_used": false
            },
            "data_5": {
              "is_used": false
            }
          }
        },
        {
          "id": 4,
          "name": "Truck Scale 1",
          "model": "TS-5000",
          "direction": "IMPORT",
          "location_id": 1,
          "location_name": "string",
          "manufacturer_id": 1,
          "manufacturer_name": "string",
          "manufacturer_code": "string",
          "protocol_id": 1,
          "protocol_name": "Modbus TCP",
          "protocol_code": "MODBUS_TCP",
          "is_active": true,
          "created_at": "2026-01-08T09:31:18.690924Z",
          "created_by": "admin",
          "updated_at": "2026-01-08T09:31:18.690924Z",
          "updated_by": "admin",
          "scale_config": {
            "protocol": "MODBUS_TCP",
            "scale_id": 4,
            "poll_interval": 1000,
            "conn_params": {
              "ip": "192.168.1.10",
              "port": 502
            },
            "data_1": {
              "name": "Weight",
              "is_used": true,
              "num_registers": 2,
              "start_registers": 40001
            },
            "data_2": {
              "additionalProp1": {},
              "additionalProp2": {},
              "additionalProp3": {}
            },
            "data_3": {
              "additionalProp1": {},
              "additionalProp2": {},
              "additionalProp3": {}
            },
            "data_4": {
              "additionalProp1": {},
              "additionalProp2": {},
              "additionalProp3": {}
            },
            "data_5": {
              "additionalProp1": {},
              "additionalProp2": {},
              "additionalProp3": {}
            }
          }
        },
        {
          "id": 5,
          "name": "Truck Scale 2",
          "model": "TS-5000",
          "direction": "IMPORT",
          "location_id": 1,
          "location_name": "string",
          "manufacturer_id": 1,
          "manufacturer_name": "string",
          "manufacturer_code": "string",
          "protocol_id": 3,
          "protocol_name": "Modbus RTU",
          "protocol_code": "MODBUS_RTU",
          "is_active": true,
          "created_at": "2026-01-08T09:48:59.183752Z",
          "created_by": "admin",
          "updated_at": "2026-01-08T09:48:59.183752Z",
          "updated_by": "admin",
          "scale_config": {
            "protocol": "MODBUS_RTU",
            "scale_id": 5,
            "poll_interval": 1000,
            "conn_params": {
              "parity": "even",
              "unit_id": 1,
              "com_port": "COM3",
              "baud_rate": 9600,
              "data_bits": 8,
              "stop_bits": 1
            },
            "data_1": {
              "name": "Weight",
              "is_used": true,
              "data_type": "int32",
              "byte_order": "big_endian",
              "function_code": 3,
              "num_registers": 2,
              "start_register": 0
            },
            "data_2": {
              "is_used": false
            },
            "data_3": {
              "is_used": false
            },
            "data_4": {
              "is_used": false
            },
            "data_5": {
              "is_used": false
            }
          }
        }
      ],
      "page": 0,
      "size": 10,
      "total_elements": 4,
      "total_pages": 1,
      "is_first": true,
      "is_last": true,
      "has_next": false,
      "has_previous": false
    },
    "total_scales": 4,
    "active_scales": 4,
    "inactive_scales": 0
  }
}
```
## POST http://localhost:8080/api/v1/reports/interval/v2

* Request body: 
``` json
{
  "scaleIds": [
    0
  ],
  "manufacturerIds": [
    0
  ],
  "locationIds": [
    0
  ],
  "direction": "string - loại cân nhập hay xuất",
  "shiftIds": [
    0
  ],
  "fromTime": "2026-01-13T07:44:12.600Z",
  "toTime": "2026-01-13T07:44:12.600Z",
  "interval": "SHIFT",
  "aggregationByField": {
    "additionalProp1": "SUM",
    "additionalProp2": "SUM",
    "additionalProp3": "SUM"
  },
  "ratioFormula": "string, chỗ này là một trường thống kê, tính toán dựa trên công thức, mặc định data_1/data_3",
  "page": 0,
  "size": 0
}
```
* Response Code: 200
* Response Body:
``` json
{
  "success": true,
  "data": {
    "data": [
      {
        "interval": "DAY",
        "fromDate": "2025-01-13T07:22:50.619Z",
        "toDate": "2026-01-13T07:22:50.619Z",
        "dataFieldNames": {
          "data_5": "Data 5",
          "data_2": "Data 2",
          "data_1": "Weight",
          "data_4": "Data 4",
          "data_3": "WeightJHGSJKF"
        },
        "aggregationByField": {},
        "ratioFormula": "data_1/data_2",
        "overview": {
          "0": {
            "data_1": {
              "value": "1769500.00",
              "aggregation": "SUM",
              "name": "Weight",
              "used": true
            },
            "data_3": {
              "value": "0.00",
              "aggregation": "AVG",
              "name": "WeightJHGSJKF",
              "used": true
            }
          }
        },
        "rows": [
          {
            "scale": {
              "id": 1,
              "name": "Cân 11",
              "model": "IND570",
              "location": {
                "id": 1,
                "code": "WS_01",
                "name": "string",
                "description": "string"
              },
              "is_active": true,
              "created_at": "2025-12-24T07:11:45.002195Z",
              "created_by": "admin",
              "updated_at": "2025-12-25T07:48:44.222954Z",
              "updated_by": "admin"
            },
            "period": "2025-12-25",
            "recordCount": 1048,
            "direction": 0,
            "ratio": {
              "value": "26215.0000",
              "formula": "data_1/data_2"
            },
            "start_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "4",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "131075",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "end_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "31",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "2031647",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "data_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "5",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "131075",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            }
          },
          {
            "scale": {
              "id": 1,
              "name": "Cân 11",
              "model": "IND570",
              "location": {
                "id": 1,
                "code": "WS_01",
                "name": "string",
                "description": "string"
              },
              "is_active": true,
              "created_at": "2025-12-24T07:11:45.002195Z",
              "created_by": "admin",
              "updated_at": "2025-12-25T07:48:44.222954Z",
              "updated_by": "admin"
            },
            "period": "2025-12-26",
            "recordCount": 20,
            "direction": 0,
            "ratio": {
              "value": "0",
              "formula": "data_1/data_2"
            },
            "start_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "4",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "131075",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "end_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "31",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "2031647",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "data_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "0",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "0",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            }
          },
          {
            "scale": {
              "id": 1,
              "name": "Cân 11",
              "model": "IND570",
              "location": {
                "id": 1,
                "code": "WS_01",
                "name": "string",
                "description": "string"
              },
              "is_active": true,
              "created_at": "2025-12-24T07:11:45.002195Z",
              "created_by": "admin",
              "updated_at": "2025-12-25T07:48:44.222954Z",
              "updated_by": "admin"
            },
            "period": "2025-12-29",
            "recordCount": 2,
            "direction": 0,
            "ratio": {
              "value": "0",
              "formula": "data_1/data_2"
            },
            "start_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "4",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "131075",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "end_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "31",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "2031647",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "data_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "0",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "0",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            }
          },
          {
            "scale": {
              "id": 1,
              "name": "Cân 11",
              "model": "IND570",
              "location": {
                "id": 1,
                "code": "WS_01",
                "name": "string",
                "description": "string"
              },
              "is_active": true,
              "created_at": "2025-12-24T07:11:45.002195Z",
              "created_by": "admin",
              "updated_at": "2025-12-25T07:48:44.222954Z",
              "updated_by": "admin"
            },
            "period": "2025-12-30",
            "recordCount": 7,
            "direction": 0,
            "ratio": {
              "value": "65537.0000",
              "formula": "data_1/data_2"
            },
            "start_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "4",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "131075",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "end_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "31",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "2031647",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            },
            "data_values": {
              "data_5": {
                "value": "0",
                "name": "Data 5",
                "used": false
              },
              "data_2": {
                "value": "25",
                "name": "Data 2",
                "used": false
              },
              "data_1": {
                "value": "1638425",
                "name": "Weight",
                "used": true
              },
              "data_4": {
                "value": "0",
                "name": "Data 4",
                "used": false
              },
              "data_3": {
                "value": "0",
                "name": "WeightJHGSJKF",
                "used": true
              }
            }
          }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 4,
    "total_pages": 1,
    "is_first": true,
    "is_last": true,
    "has_next": false,
    "has_previous": false
  }
}
```

## POST http://localhost:8080/api/v1/reports/export/v2

Xuất file báo cáo interval/v2 với template. API này kết hợp tham số của `/interval/v2` với template export của `/reports/export`.

* Request body: 
``` json
{
  "importId": 1,
  "scaleIds": [1, 2, 3],
  "manufacturerIds": [1, 2],
  "locationIds": [1],
  "direction": "IMPORT",
  "shiftIds": [1, 2],
  "fromTime": "2026-01-01T00:00:00Z",
  "toTime": "2026-01-13T23:59:59Z",
  "interval": "DAY",
  "aggregationByField": {
    "data_1": "SUM",
    "data_2": "AVG",
    "data_3": "SUM"
  },
  "ratioFormula": "data_1/data_3",
  "page": 0,
  "size": 1000
}
```

**Mô tả các trường:**
- `importId` (required): ID của template đã import (từ bảng `template_imports`)
- `scaleIds`: Mảng ID các cân cần báo cáo
- `manufacturerIds`: Mảng ID nhà sản xuất để lọc
- `locationIds`: Mảng ID địa điểm để lọc  
- `direction`: Hướng cân - "IMPORT" (nhập) hoặc "EXPORT" (xuất)
- `shiftIds`: Mảng ID ca làm việc để lọc (chỉ áp dụng khi interval là SHIFT)
- `fromTime` (required): Thời gian bắt đầu
- `toTime` (required): Thời gian kết thúc
- `interval` (required): Khoảng thời gian - HOUR, DAY, WEEK, MONTH, YEAR, SHIFT
- `aggregationByField`: Phương thức tổng hợp cho từng trường dữ liệu (SUM, AVG, MAX, MIN)
- `ratioFormula`: Công thức tính tỷ lệ (mặc định: "data_1/data_3")
- `page`, `size`: Phân trang (không ảnh hưởng đến export, luôn xuất tất cả)

* Response Code: 200
* Response: File download (Word/PDF/Excel tùy theo template)
* Response Headers:
  - `Content-Disposition: attachment; filename="IntervalReportV2_DAY_20260101_20260113.docx"`
  - `X-Import-Id: 1`
  - `X-Execution-Time-Ms: 1523`
  - `Content-Type`: Tùy theo loại file template (docx/pdf/xlsx)

**Cấu trúc dữ liệu trong template:**

### Thông tin chung (Organization Info)
- `{{organizationName}}`: Tên công ty
- `{{companyNameEn}}`: Tên công ty (tiếng Anh)
- `{{address}}`: Địa chỉ
- `{{phone}}`: Số điện thoại
- `{{email}}`: Email
- `{{taxCode}}`: Mã số thuế
- `{{logoData}}`: Logo công ty (binary data)

### Thông tin báo cáo
- `{{reportTitle}}`: "Báo cáo khoảng thời gian V2"
- `{{reportCode}}`: "INTERVAL_V2"
- `{{startTime}}`: Thời gian bắt đầu (dd/MM/yyyy)
- `{{endTime}}`: Thời gian kết thúc (dd/MM/yyyy)
- `{{exportTime}}`: Thời gian xuất báo cáo (dd/MM/yyyy HH:mm:ss)
- `{{preparedBy}}`: Người tạo báo cáo
- `{{scaleNames}}`: Danh sách tên các cân

### Tên cột dữ liệu
- `{{data1Name}}`: Tên trường data_1 (VD: "Khối lượng")
- `{{data2Name}}`: Tên trường data_2 (VD: "Nhiệt độ")
- `{{data3Name}}`: Tên trường data_3
- `{{data4Name}}`: Tên trường data_4
- `{{data5Name}}`: Tên trường data_5

### Bảng dữ liệu (Table Structure)
Header bảng nên có các cột:
- **Số thứ tự**: `{{rowNumber}}`
- **Tên cân**: `{{scaleName}}`
- **Địa điểm**: `{{location}}`
- **Thời gian**: `{{period}}`
- **Loại cân**: `{{direction}}` (0=Unknown, 1=Nhập, 2=Xuất)

**Cho mỗi data field (n = 1 đến 5), có 3 cột:**
- **{dataNName} - Bắt đầu**: `{{dataNStart}}` - Giá trị tại thời điểm bắt đầu (từ start_values)
- **{dataNName} - Kết thúc**: `{{dataNEnd}}` - Giá trị tại thời điểm kết thúc (từ end_values)
- **{dataNName} - Giá trị**: `{{dataNTotal}}` - Giá trị tổng hợp (từ data_values)

**Ví dụ cụ thể cho data_1:**
- **{{data1Name}} - Bắt đầu**: `{{data1Start}}`
- **{{data1Name}} - Kết thúc**: `{{data1End}}`
- **{{data1Name}} - Giá trị**: `{{data1Total}}`

- **Số bản ghi**: `{{recordCount}}`

**Lưu ý về bảng:** 
- Template Word (.docx): Sử dụng vòng lặp `{{#rows}}...{{/rows}}` để lặp qua từng hàng
- Template PDF (.html): Sử dụng Thymeleaf `th:each="row : ${rows}"`
- Template Excel (.xlsx): Sử dụng row template với placeholders

### Phần Overview/Summary (Statistics)

**Tổng hợp chung (summary):**
- `{{summary.totalScales}}`: Tổng số cân
- `{{summary.totalRecords}}`: Tổng số bản ghi
- `{{summary.data1GrandTotal}}`: Tổng cộng data_1 (tất cả direction)
- `{{summary.data2GrandTotal}}`: Tổng cộng data_2 (tất cả direction)
- `{{summary.data3GrandTotal}}`: Tổng cộng data_3 (tất cả direction)
- `{{summary.data4GrandTotal}}`: Tổng cộng data_4 (tất cả direction)
- `{{summary.data5GrandTotal}}`: Tổng cộng data_5 (tất cả direction)
- `{{summary.data1Average}}`: Trung bình data_1
- `{{summary.data2Average}}`: Trung bình data_2
- `{{summary.data3Average}}`: Trung bình data_3
- `{{summary.data4Average}}`: Trung bình data_4
- `{{summary.data5Average}}`: Trung bình data_5

**Thống kê theo hướng cân (directionSummaries):**

Dữ liệu được group theo direction code: "0" (Unknown), "1" (Nhập), "2" (Xuất)

**Cân chưa rõ (directionSummaries.0):**
- `{{directionSummaries.0.directionCode}}`: 0
- `{{directionSummaries.0.directionName}}`: "Unknown"
- `{{directionSummaries.0.totalScales}}`: Số cân không xác định hướng
- `{{directionSummaries.0.totalRecords}}`: Số bản ghi
- `{{directionSummaries.0.data1GrandTotal}}`: Tổng data_1
- `{{directionSummaries.0.data1Average}}`: Trung bình data_1
- *(Tương tự cho data_2 đến data_5)*

**Cân nhập (directionSummaries.1):**
- `{{directionSummaries.1.directionCode}}`: 1
- `{{directionSummaries.1.directionName}}`: "Nhập"
- `{{directionSummaries.1.totalScales}}`: Số cân nhập
- `{{directionSummaries.1.totalRecords}}`: Số bản ghi nhập
- `{{directionSummaries.1.data1GrandTotal}}`: Tổng data_1 cân nhập
- `{{directionSummaries.1.data2GrandTotal}}`: Tổng data_2 cân nhập
- `{{directionSummaries.1.data3GrandTotal}}`: Tổng data_3 cân nhập
- `{{directionSummaries.1.data4GrandTotal}}`: Tổng data_4 cân nhập
- `{{directionSummaries.1.data5GrandTotal}}`: Tổng data_5 cân nhập
- `{{directionSummaries.1.data1Average}}`: Trung bình data_1 cân nhập
- `{{directionSummaries.1.data2Average}}`: Trung bình data_2 cân nhập
- `{{directionSummaries.1.data3Average}}`: Trung bình data_3 cân nhập
- `{{directionSummaries.1.data4Average}}`: Trung bình data_4 cân nhập
- `{{directionSummaries.1.data5Average}}`: Trung bình data_5 cân nhập

**Cân xuất (directionSummaries.2):**
- `{{directionSummaries.2.directionCode}}`: 2
- `{{directionSummaries.2.directionName}}`: "Xuất"
- `{{directionSummaries.2.totalScales}}`: Số cân xuất
- `{{directionSummaries.2.totalRecords}}`: Số bản ghi xuất
- `{{directionSummaries.2.data1GrandTotal}}`: Tổng data_1 cân xuất
- `{{directionSummaries.2.data2GrandTotal}}`: Tổng data_2 cân xuất
- `{{directionSummaries.2.data3GrandTotal}}`: Tổng data_3 cân xuất
- `{{directionSummaries.2.data4GrandTotal}}`: Tổng data_4 cân xuất
- `{{directionSummaries.2.data5GrandTotal}}`: Tổng data_5 cân xuất
- `{{directionSummaries.2.data1Average}}`: Trung bình data_1 cân xuất
- `{{directionSummaries.2.data2Average}}`: Trung bình data_2 cân xuất
- `{{directionSummaries.2.data3Average}}`: Trung bình data_3 cân xuất
- `{{directionSummaries.2.data4Average}}`: Trung bình data_4 cân xuất
- `{{directionSummaries.2.data5Average}}`: Trung bình data_5 cân xuất

**Lưu ý:**
- Nếu không có cân nào với direction code nào đó, key tương ứng sẽ không xuất hiện trong `directionSummaries`
- Có thể loop qua directionSummaries để hiển thị động: `{{#directionSummaries}}...{{/directionSummaries}}`

### Ví dụ Template Word (.docx)

```
[LOGO]
TÊN CÔNG TY: {{organizationName}}
Địa chỉ: {{address}}
Điện thoại: {{phone}}

BÁO CÁO KHOẢNG THỜI GIAN V2
Từ ngày: {{startTime}} đến {{endTime}}
Cân: {{scaleNames}}

BẢNG DỮ LIỆU:
| STT | Tên cân | Địa điểm | Thời gian | Loại | {{data1Name}}-Bắt đầu | {{data1Name}}-Kết thúc | {{data1Name}}-Giá trị | Số bản ghi |
|-----|---------|----------|-----------|------|---------------------|---------------------|---------------------|------------|
{{#rows}}
| {{rowNumber}} | {{scaleName}} | {{location}} | {{period}} | {{direction}} | {{data1Start}} | {{data1End}} | {{data1Total}} | {{recordCount}} |
{{/rows}}

TỔNG HỢP CHUNG:
- Tổng số cân: {{summary.totalScales}}
- Tổng số bản ghi: {{summary.totalRecords}}
- Tổng {{data1Name}}: {{summary.data1GrandTotal}}
- Trung bình {{data1Name}}: {{summary.data1Average}}

THỐNG KÊ THEO LOẠI CÂN:

Cân Nhập (Import):
- Số cân nhập: {{directionSummaries.1.totalScales}}
- Số bản ghi: {{directionSummaries.1.totalRecords}}
- Tổng {{data1Name}}: {{directionSummaries.1.data1GrandTotal}}
- Trung bình {{data1Name}}: {{directionSummaries.1.data1Average}}

Cân Xuất (Export):
- Số cân xuất: {{directionSummaries.2.totalScales}}
- Số bản ghi: {{directionSummaries.2.totalRecords}}
- Tổng {{data1Name}}: {{directionSummaries.2.data1GrandTotal}}
- Trung bình {{data1Name}}: {{directionSummaries.2.data1Average}}

Cân chưa rõ:
- Số cân: {{directionSummaries.0.totalScales}}
- Số bản ghi: {{directionSummaries.0.totalRecords}}

Người lập: {{preparedBy}}
Thời gian xuất: {{exportTime}}
```

**Lưu ý kỹ thuật:**
- API sẽ gọi `/interval/v2` để lấy dữ liệu thống kê đầy đủ
- Dữ liệu được chuyển đổi từ cấu trúc `IntervalReportResponseDtoV2` sang `ReportData`
- **Mỗi data field có 3 giá trị**: Start (từ start_values), End (từ end_values), Total (từ data_values)
- **Overview được chia theo direction**: directionSummaries với key "0", "1", "2"
- **Summary tổng hợp**: Tổng từ tất cả directions
- Hỗ trợ đầy đủ các định dạng: Word (.docx), PDF (.html), Excel (.xlsx)

**Mapping dữ liệu từ interval/v2:**
- `start_values` → `data1Start`, `data2Start`, ..., `data5Start`
- `end_values` → `data1End`, `data2End`, ..., `data5End`
- `data_values` → `data1Total`, `data2Total`, ..., `data5Total`
- `direction` → `direction` (0/1/2)
- `overview["0"]`, `overview["1"]`, `overview["2"]` → `directionSummaries.0`, `directionSummaries.1`, `directionSummaries.2`