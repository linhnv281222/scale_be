## GET HISTORY

### GET http://localhost:8080/api/v1/weighing-history
* Parameter:
`page`:Integer- 0 | Số trang (0-indexed) |
`size`:Integer- 20 | Số bản ghi trên mỗi trang |
`sort`:String- `createdAt,desc` | Sắp xếp theo trường (format: `field,direction`) |
`search`:String- Tìm kiếm toàn cục trên tên cân và model |
`scaleId`:Long- Lọc theo ID của cân |
`scaleCode`:String- Lọc theo mã/tên cân (tìm kiếm gần đúng) |
`direction`:String- Lọc theo loại cân: `IMPORT` (Nhập) hoặc `EXPORT` (Xuất) |
`locationId`:Long- Lọc theo ID vị trí |
`protocolId`:Long- Lọc theo ID giao thức |
`startTime`:OffsetDateTime- Lọc từ thời điểm này (ISO 8601 format) |
`endTime`:OffsetDateTime- Lọc đến thời điểm này (ISO 8601 format) |

* Response code: 200

* Response body:
``` json
{
  "content": [
    {
      "scaleId": 2,
      "scaleCode": "Cân 12",
      "scaleName": "Cân 12",
      "locationName": "Xưởng A",
      "createdAt": "2025-12-26T04:02:22.765026Z",
      "lastTime": "2025-12-26T04:02:22.727731Z",
      "data1": "0",
      "data2": "0",
      "dataValues": {
        "data_1": {
          "value": "0",
          "name": "Weight",
          "used": true
        },
        "data_2": {
          "value": "0",
          "name": "Status",
          "used": true
        },
        "data_3": {
          "value": null,
          "name": "Data 3",
          "used": false
        },
        "data_4": {
          "value": null,
          "name": "Data 4",
          "used": false
        },
        "data_5": {
          "value": null,
          "name": "Data 5",
          "used": false
        }
      }
    },
    {
      "scaleId": 1,
      "scaleCode": "Cân 11",
      "scaleName": "Cân 11",
      "locationName": "Xưởng A",
      "createdAt": "2025-12-26T04:02:22.722209Z",
      "lastTime": "2025-12-26T04:02:22.683584Z",
      "data1": "0",
      "data2": "0",
      "dataValues": {
        "data_1": {
          "value": "0",
          "name": "Weight",
          "used": true
        },
        "data_2": {
          "value": "0",
          "name": "Status",
          "used": true
        },
        "data_3": {
          "value": null,
          "name": "Data 3",
          "used": false
        },
        "data_4": {
          "value": null,
          "name": "Data 4",
          "used": false
        },
        "data_5": {
          "value": null,
          "name": "Data 5",
          "used": false
        }
      }
    },
    {
      "scaleId": 2,
      "scaleCode": "Cân 12",
      "scaleName": "Cân 12",
      "locationName": "Xưởng A",
      "createdAt": "2025-12-26T04:02:12.735903Z",
      "lastTime": "2025-12-26T04:02:12.694957Z",
      "data1": "0",
      "data2": "0",
      "dataValues": {
        "data_1": {
          "value": "0",
          "name": "Weight",
          "used": true
        },
        "data_2": {
          "value": "0",
          "name": "Status",
          "used": true
        },
        "data_3": {
          "value": null,
          "name": "Data 3",
          "used": false
        },
        "data_4": {
          "value": null,
          "name": "Data 4",
          "used": false
        },
        "data_5": {
          "value": null,
          "name": "Data 5",
          "used": false
        }
      }
    }
  ],
  "page": 0,
  "size": 3,
  "total_elements": 1809,
  "total_pages": 603,
  "is_first": true,
  "is_last": false,
  "has_next": true,
  "has_previous": false
}
```
### POST http://localhost:8080/api/v1/reports/interval - Chỗ này là data tổng hợp theo từng khoảng thời gian, có thể dùng để dựng biểu đồ thổng hợp
* Payload:
``` json
{
  "scaleIds": [
    0
  ],
  "manufacturerId": 0,
  "locationId": 0,
  "direction": "string",
  "fromDate": "2026-01-08",
  "toDate": "2026-01-08",
  "fromTime": "2026-01-08T16:32:02.341Z",
  "toTime": "2026-01-08T16:32:02.341Z",
  "interval": "SHIFT",
  "aggregationByField": {
    "additionalProp1": "SUM",
    "additionalProp2": "SUM",
    "additionalProp3": "SUM"
  }
}
```

* Response code: 200
* Response body:
``` json
{
  "success": true,
  "data": {
    "interval": "WEEK",
    "fromDate": "2025-01-08",
    "toDate": "2026-01-08",
    "dataFieldNames": {
      "data_5": "Data 5",
      "data_2": "Status",
      "data_1": "Weight",
      "data_4": "Data 4",
      "data_3": "Data 3"
    },
    "aggregationByField": {
      "data_5": "ABS",
      "data_2": "ABS",
      "data_1": "SUM",
      "data_4": "ABS",
      "data_3": "SUM"
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
            "name": "Xưởng A"
          },
          "is_active": true,
          "created_at": "2025-12-24T07:11:45.002195Z",
          "created_by": "admin",
          "updated_at": "2025-12-25T07:48:44.222954Z",
          "updated_by": "admin"
        },
        "period": "2025-12-22",
        "recordCount": 1068,
        "data_values": {
          "data_5": {
            "value": "0",
            "name": "Data 5",
            "used": false
          },
          "data_2": {
            "value": "5",
            "name": "Status",
            "used": true
          },
          "data_1": {
            "value": "98437322",
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
            "name": "Data 3",
            "used": false
          }
        }
      },
      {
        "scale": {
          "id": 2,
          "name": "Cân 12",
          "model": "NMH002",
          "location": {
            "id": 1,
            "code": "WS_01",
            "name": "Xưởng A"
          },
          "is_active": true,
          "created_at": "2025-12-25T08:13:43.377903Z",
          "created_by": "admin",
          "updated_at": "2025-12-25T08:13:43.377903Z",
          "updated_by": "admin"
        },
        "period": "2025-12-22",
        "recordCount": 741,
        "data_values": {
          "data_5": {
            "value": "0",
            "name": "Data 5",
            "used": false
          },
          "data_2": {
            "value": "4",
            "name": "Status",
            "used": true
          },
          "data_1": {
            "value": "105321167",
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
            "name": "Data 3",
            "used": false
          }
        }
      }
    ]
  }
}
```

## WEBSOCKET EVENTS

**Endpoint:** `ws://localhost:8080/ws-scalehub`  
**Protocol:** STOMP over SockJS  
**Prefix:** `/topic`

---

## 1. EVENT DỮ LIỆU ĐO CÂN

### 1.1. Topic: `/topic/scales` - Đây là event cũ, có thể bỏ qua cái này
**Mô tả:** Nhận dữ liệu đo từ TẤT CẢ các cân trong hệ thống (broadcast toàn cục)  
**Tần suất:** Real-time (mỗi khi có dữ liệu mới từ bất kỳ cân nào)  
**Event Type:** `MeasurementEvent` (single object, NOT array)

**Cấu trúc dữ liệu:**
```json
{
  "scaleId": 1,
  "lastTime": "2026-01-08T10:30:45+07:00",
  "status": "online",
  "data1": {
    "name": "Weight",
    "value": "125.5",
    "isUsed": true
  },
  "data2": {
    "name": "Status",
    "value": "5",
    "isUsed": true
  },
  "data3": {
    "name": "Temperature",
    "value": "25.3",
    "isUsed": true
  },
  "data4": {
    "name": "Data 4",
    "value": null,
    "isUsed": false
  },
  "data5": {
    "name": "Data 5",
    "value": null,
    "isUsed": false
  }
}
```

**Lưu ý:** Topic này gửi từng event riêng lẻ mỗi khi 1 cân có dữ liệu mới (không phải array)  
**Sử dụng:** Màn hình tổng quát hiển thị tất cả các cân

---

### 1.2. Topic: `/topic/scale/{scaleId}`
**Mô tả:** Nhận dữ liệu đo từ MỘT cân cụ thể  
**Tần suất:** Real-time (mỗi khi cân này có dữ liệu mới)  
**Event Type:** `MeasurementEvent`  
**Ví dụ topic:** `/topic/scale/1`, `/topic/scale/2`

**Cấu trúc dữ liệu:** Giống với `/topic/scales`
```json
{
  "scaleId": 1,
  "lastTime": "2026-01-08T10:30:45+07:00",
  "status": "online",
  "data1": {
    "name": "Weight",
    "value": "125.5",
    "isUsed": true
  },
  "data2": {
    "name": "Status",
    "value": "5",
    "isUsed": true
  },
  "data3": {
    "name": "Temperature",
    "value": "25.3",
    "isUsed": true
  },
  "data4": {
    "name": "Data 4",
    "value": null,
    "isUsed": false
  },
  "data5": {
    "name": "Data 5",
    "value": null,
    "isUsed": false
  }
}
```

### 1.3. Topic: `/topic/scale-summary`-
**Mô tả:** Thông tin tổng quan về số lượng và trạng thái cân  
**Tần suất:** Định kỳ mỗi 10 giây hoặc khi có thay đổi  
**Event Type:** `ScaleSummaryEvent`

**Cấu trúc dữ liệu:**
```json
{
  "totalScales": 10,
  "activeScales": 8,
  "onlineScales": 6,
  "offlineScales": 2,
  "timestamp": "2026-01-08T10:30:45+07:00"
}
```

**Sử dụng:** Dashboard header, thống kê tổng quan

---

### 1.4. Topic: `/topic/all-scales-data`
**Mô tả:** Snapshot dữ liệu của TẤT CẢ các cân (gửi định kỳ)  
**Tần suất:** Mỗi 5-10 giây  
**Event Type:** `AllScalesDataEvent`

**Cấu trúc dữ liệu:**
```json
{
  "timestamp": "2026-01-08T10:30:45+07:00",
  "scales": [
    {
      "scaleId": 1,
      "scaleName": "Cân 11",
      "status": "online",
      "lastTime": "2026-01-08T10:30:44+07:00",
      "data1": {
        "name": "Weight",
        "value": "125.5",
        "isUsed": true
      },
      "data2": {
        "name": "Status",
        "value": "5",
        "isUsed": true
      },
      "data3": {
        "name": "Data 3",
        "value": null,
        "isUsed": false
      },
      "data4": {
        "name": "Data 4",
        "value": null,
        "isUsed": false
      },
      "data5": {
        "name": "Data 5",
        "value": null,
        "isUsed": false
      }
    },
    {
      "scaleId": 2,
      "scaleName": "Cân 12",
      "status": "online",
      "lastTime": "2026-01-08T10:30:43+07:00",
      "data1": {
        "name": "Weight",
        "value": "98.2",
        "isUsed": true
      },
      "data2": {
        "name": "Status",
        "value": "4",
        "isUsed": true
      },
      "data3": {
        "name": "Data 3",
        "value": null,
        "isUsed": false
      },
      "data4": {
        "name": "Data 4",
        "value": null,
        "isUsed": false
      },
      "data5": {
        "name": "Data 5",
        "value": null,
        "isUsed": false
      }
    }
  ]
}
```

**Sử dụng:** Đồng bộ dữ liệu ban đầu khi vừa kết nối WebSocket

---

## 2. EVENT CẢNH BÁO (Alert Events)

### 2.1. Topic: `/topic/scale-alerts`
**Mô tả:** Cảnh báo khi có vấn đề với cân (batch alerts)  
**Tần suất:** Mỗi 30 giây hoặc khi phát hiện vấn đề mới  
**Event Type:** `ScaleBatchAlertEvent`

**Cấu trúc dữ liệu:**
```json
{
  "timestamp": "2026-01-08T10:30:45+07:00",
  "totalIssues": 3,
  "isRecoveryBatch": false,
  "summary": {
    "criticalCount": 1,
    "warningCount": 2,
    "infoCount": 0,
    "recoveryCount": 0,
    "zeroValueCount": 1,
    "noSignalCount": 0,
    "connectionLostCount": 1,
    "readErrorCount": 0,
    "staleDataCount": 1,
    "otherCount": 0
  },
  "alerts": [
    {
      "alertId": 12345,
      "scale": {
        "id": 3,
        "name": "Cân 13",
        "model": "IND570",
        "ipAddress": "192.168.1.103",
        "port": 502,
        "locationName": "Xưởng B",
        "locationId": 2
      },
      "alertType": "CRITICAL",
      "issueType": "CONNECTION_LOST",
      "detectedAt": "2026-01-08T10:25:45+07:00",
      "triggeredAt": "2026-01-08T10:30:45+07:00",
      "durationSeconds": 300,
      "message": "Mất kết nối với cân 13 trong 5 phút",
      "consecutiveFailures": 30,
      "lastKnownValue": "125.5",
      "currentStatus": "offline",
      "isRecovery": false,
      "metadata": "{\"retry_count\":30,\"last_error\":\"Connection timeout\"}"
    },
    {
      "alertId": 12346,
      "scale": {
        "id": 5,
        "name": "Cân 15",
        "model": "NMH002",
        "ipAddress": "192.168.1.105",
        "port": 502,
        "locationName": "Xưởng A",
        "locationId": 1
      },
      "alertType": "WARNING",
      "issueType": "ZERO_VALUE",
      "detectedAt": "2026-01-08T10:28:45+07:00",
      "triggeredAt": "2026-01-08T10:30:45+07:00",
      "durationSeconds": 120,
      "message": "Cân 15 đọc giá trị 0 liên tục trong 2 phút",
      "consecutiveFailures": 12,
      "lastKnownValue": "0",
      "currentStatus": "online",
      "isRecovery": false,
      "metadata": "{\"zero_count\":12}"
    },
    {
      "alertId": 12347,
      "scale": {
        "id": 7,
        "name": "Cân 17",
        "model": "IND570",
        "ipAddress": "192.168.1.107",
        "port": 502,
        "locationName": "Xưởng C",
        "locationId": 3
      },
      "alertType": "WARNING",
      "issueType": "DATA_STALE",
      "detectedAt": "2026-01-08T10:26:45+07:00",
      "triggeredAt": "2026-01-08T10:30:45+07:00",
      "durationSeconds": 240,
      "message": "Dữ liệu cân 17 không được cập nhật trong 4 phút",
      "consecutiveFailures": 24,
      "lastKnownValue": "156.8",
      "currentStatus": "online",
      "isRecovery": false,
      "metadata": "{\"last_update\":\"2026-01-08T10:26:45+07:00\"}"
    }
  ]
}
```

### Alert Types (Loại Cảnh Báo)

| Alert Type | Mô tả | Cấp độ |
|------------|-------|---------|
| `CRITICAL` | Nguy kịch - cần xử lý ngay lập tức | Đỏ |
| `WARNING` | Cảnh báo - cần chú ý | Vàng |
| `INFO` | Thông tin | Xanh |
| `RECOVERY` | Phục hồi - vấn đề đã được giải quyết | Xanh lá |

### Issue Types (Loại Vấn Đề)

| Issue Type | Mô tả | Giải pháp |
|------------|-------|-----------|
| `ZERO_VALUE` | Giá trị luôn = 0 (trạng thái dừng) | Kiểm tra cân có đang hoạt động không |
| `NO_SIGNAL` | Không có tín hiệu từ thiết bị | Kiểm tra nguồn điện, cáp kết nối |
| `CONNECTION_LOST` | Mất kết nối mạng | Kiểm tra mạng, IP, cổng kết nối |
| `TIMEOUT` | Timeout khi đọc dữ liệu | Kiểm tra tốc độ mạng, cấu hình timeout |
| `READ_ERROR` | Lỗi khi đọc dữ liệu | Kiểm tra giao thức, cấu hình |
| `PROTOCOL_ERROR` | Lỗi giao thức truyền thông | Kiểm tra cấu hình protocol |
| `HARDWARE_ERROR` | Lỗi phần cứng | Liên hệ bảo trì |
| `NETWORK_ERROR` | Lỗi mạng | Kiểm tra infrastructure mạng |
| `DATA_STALE` | Dữ liệu cũ (không cập nhật) | Kiểm tra engine đọc dữ liệu |
| `UNKNOWN` | Không xác định | Kiểm tra log chi tiết |

---

### 2.2. Recovery Alerts (Cảnh Báo Phục Hồi)
Khi vấn đề được giải quyết, hệ thống sẽ gửi recovery alert:

```json
{
  "timestamp": "2026-01-08T10:35:45+07:00",
  "totalIssues": 1,
  "isRecoveryBatch": true,
  "summary": {
    "criticalCount": 0,
    "warningCount": 0,
    "infoCount": 0,
    "recoveryCount": 1,
    "zeroValueCount": 0,
    "noSignalCount": 0,
    "connectionLostCount": 0,
    "readErrorCount": 0,
    "staleDataCount": 0,
    "otherCount": 0
  },
  "alerts": [
    {
      "alertId": 12345,
      "scale": {
        "id": 3,
        "name": "Cân 13",
        "model": "IND570",
        "ipAddress": "192.168.1.103",
        "port": 502,
        "locationName": "Xưởng B",
        "locationId": 2
      },
      "alertType": "RECOVERY",
      "issueType": "CONNECTION_LOST",
      "detectedAt": "2026-01-08T10:25:45+07:00",
      "triggeredAt": "2026-01-08T10:35:45+07:00",
      "durationSeconds": 600,
      "message": "Cân 13 đã kết nối lại thành công",
      "consecutiveFailures": 0,
      "lastKnownValue": "125.5",
      "currentStatus": "online",
      "isRecovery": true,
      "metadata": "{\"recovered_at\":\"2026-01-08T10:35:45+07:00\"}"
    }
  ]
}