# Hướng dẫn test cấu hình cân theo từng giao thức

## 1. Mục tiêu

Tài liệu này hướng dẫn kiểm tra chức năng cấu hình cân và xác nhận hệ thống đọc đúng dữ liệu từ thiết bị đối với các giao thức đang có trong backend:

| Giao thức | Giá trị `protocol` nên dùng | Kết nối |
|---|---|---|
| Modbus TCP | `MODBUS_TCP` | Ethernet/TCP |
| Modbus RTU | `MODBUS_RTU` | Serial RS232/RS485 |
| S-Bus dạng frame | `SBUS` | Serial |
| SABUS ASCII | `SABUS` | Serial, lệnh `FL`/`NT` |
| Siemens S7 | `SIEMENS_S7` | ISO-on-TCP, mặc định cổng 102 |
| Serial ASCII tổng quát | `SERIAL` | Serial, streaming hoặc request/response |

> `SERIAL` dùng cho thiết bị ASCII không thuộc Modbus/S-Bus/SABUS. Engine hỗ trợ thiết bị tự phát frame liên tục và thiết bị cần gửi lệnh trước khi đọc.

## 2. Cách hệ thống áp dụng cấu hình

Backend hiện **không có API test connection riêng**. Quy trình test tích hợp là:

1. Tạo hoặc cập nhật một cân với `is_active: true`.
2. Backend tự hot-reload engine tương ứng với trường `protocol`.
3. Engine kết nối thiết bị và đọc theo `poll_interval`.
4. Dữ liệu thành công được phát qua WebSocket, cập nhật current state và lưu vào weighing history.

Hai trường giao thức có ý nghĩa khác nhau:

- `protocol_id`: liên kết tới danh mục protocol, dùng để hiển thị/lọc.
- `protocol`: chuỗi trong `scale_config`, được `EngineFactory` dùng để chọn engine thực sự.

Khi test phải bảo đảm hai trường cùng chỉ một giao thức. Ví dụ protocol có mã `MODBUS_TCP` thì `protocol_id` phải là ID của bản ghi đó và `protocol` phải là `MODBUS_TCP`.

## 3. Chuẩn bị

### 3.1. Điều kiện chung

- Backend chạy tại `http://localhost:8080/api/v1`.
- Có tài khoản `ADMIN` hoặc tài khoản có quyền `CONFIG_MANAGE` để sửa cấu hình.
- Đã có `location_id` hợp lệ.
- Thiết bị hoặc simulator có thể truy cập từ đúng máy đang chạy backend.
- Firewall cho phép cổng TCP cần test.
- Với Serial, biết đúng tên cổng trên **máy backend**, ví dụ `COM3` trên Windows hoặc `/dev/ttyUSB0` trên Linux.
- Không mở cùng cổng COM bằng phần mềm khác trong lúc ScaleHub đang đọc.

Swagger có thể mở tại:

```text
http://localhost:8080/api/v1/swagger-ui.html
```

### 3.2. Đăng nhập và lấy token bằng PowerShell

```powershell
$baseUrl = "http://localhost:8080/api/v1"

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'

$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }
```

Tài khoản mặc định chỉ phù hợp môi trường dev; không dùng mật khẩu mặc định ở môi trường thật.

### 3.3. Lấy danh mục protocol và cân hiện có

```powershell
$protocols = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/protocols?page=0&size=100" `
  -Headers $headers

$scales = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/scales?page=0&size=100" `
  -Headers $headers
```

Ghi lại ID protocol cần test và ID cân. Nếu database chưa có protocol mới, cần chạy migration/khởi động đúng phiên bản ứng dụng trước khi test.

> Các giá trị `protocol_id` trong toàn bộ JSON bên dưới chỉ là ví dụ. Luôn thay bằng ID lấy từ `GET /protocols`; không giả định ID giống giữa các database.

### 3.4. Cấu trúc request cập nhật cân

API cập nhật là:

```http
PUT /api/v1/scales/{scaleId}
Authorization: Bearer {token}
Content-Type: application/json
```

Đây là API `PUT`, vì vậy nên gửi đầy đủ thông tin của cân, không chỉ riêng phần cấu hình:

```json
{
  "name": "CAN-TEST-01",
  "location_id": 1,
  "manufacturer_id": null,
  "protocol_id": 1,
  "model": "TEST",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "MODBUS_TCP",
  "poll_interval": 1000,
  "conn_params": {},
  "data_1": {},
  "data_2": { "is_used": false },
  "data_3": { "is_used": false },
  "data_4": { "is_used": false },
  "data_5": { "is_used": false }
}
```

`poll_interval` nhỏ nhất là 100 ms; khi test nên dùng 1000 ms để dễ theo dõi và tránh tạo quá nhiều bản ghi.

## 4. Kiểm tra kết quả đọc

Sau khi lưu cấu hình, chờ khoảng 2-5 chu kỳ đọc rồi kiểm tra theo thứ tự sau.

### 4.1. Đọc current state

```powershell
$scaleId = 1

Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/scales/current-states?scale_id=$scaleId" `
  -Headers $headers | ConvertTo-Json -Depth 10
```

Kết quả đạt khi:

- Có đúng `scaleId`.
- `status` là `ONLINE`.
- `lastTime` tiếp tục thay đổi theo chu kỳ.
- `dataValues.data_1.value` khớp dữ liệu trên thiết bị/simulator.
- Tên dữ liệu khớp trường `name` trong `data_1` đến `data_5`.

### 4.2. Đọc lịch sử

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/weighing-history?scaleId=$scaleId&page=0&size=10&sort=createdAt,desc" `
  -Headers $headers | ConvertTo-Json -Depth 10
```

Xác nhận bản ghi mới có `data1` đến `data5`, `lastTime` và thông tin cân đúng.

### 4.3. Kiểm tra realtime qua WebSocket

- SockJS/STOMP endpoint: `http://localhost:8080/api/v1/ws-scalehub`
- Topic riêng cho cân: `/topic/scale/{scaleId}`
- Topic tất cả cân: `/topic/scales`

Event thành công có dạng:

```json
{
  "scaleId": 1,
  "lastTime": "2026-10-14T10:30:00+07:00",
  "data1": {
    "name": "Weight",
    "value": "123.45"
  },
  "status": "ONLINE"
}
```

### 4.4. Kiểm tra log backend

Tìm các dòng tương ứng:

```text
[EngineReload] Reloading engine for scale ...
[EngineManager] Started engine for scale ...
Connected to ...
[CORE-Worker-...] Received measurement from Scale ID: ...
```

Nếu thất bại, log thường cho biết lỗi kết nối, timeout, sai thanh ghi/tag hoặc sai định dạng dữ liệu.

## 5. Test Modbus TCP

### 5.1. Cấu hình mẫu

Thay IP, Unit ID, địa chỉ thanh ghi và kiểu dữ liệu theo thiết bị:

```json
{
  "name": "CAN-MODBUS-TCP",
  "location_id": 1,
  "manufacturer_id": null,
  "protocol_id": 1,
  "model": "MODBUS-TCP-TEST",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "MODBUS_TCP",
  "poll_interval": 1000,
  "conn_params": {
    "ip": "192.168.1.10",
    "port": 502,
    "unit_id": 1,
    "response_timeout_ms": 5000
  },
  "data_1": {
    "name": "Weight",
    "is_used": true,
    "function_code": 3,
    "start_registers": 0,
    "num_registers": 2,
    "data_type": "FLOAT32",
    "byte_order": "BIG_ENDIAN",
    "word_order": "BIG_ENDIAN",
    "decimal_places": 2
  },
  "data_2": { "is_used": false },
  "data_3": { "is_used": false },
  "data_4": { "is_used": false },
  "data_5": { "is_used": false }
}
```

### 5.2. Điểm cần kiểm tra

- `ip`: IP của gateway/cân Modbus TCP.
- `port`: thường là 502.
- `unit_id`: Slave/Unit ID; nếu bỏ trống engine dùng 1, nhưng nên khai báo rõ.
- `function_code`: chỉ hỗ trợ 3 (Holding Registers) hoặc 4 (Input Registers).
- `start_registers`: offset thanh ghi mà thư viện gửi xuống thiết bị.
- `num_registers`: số register 16-bit cần đọc.

**Lưu ý địa chỉ:** engine truyền nguyên `start_registers` cho thư viện, không tự trừ 40001/30001. Nếu tài liệu thiết bị ghi Holding Register `40001` theo cách đánh số tham chiếu, thông thường phải nhập offset `0`; `40002` nhập `1`. Luôn đối chiếu tài liệu thiết bị hoặc simulator trước khi kết luận.

Các `data_type` hỗ trợ gồm:

```text
BOOL, INT16, UINT16, INT32, UINT32, INT64, UINT64,
FLOAT32, FLOAT64, STRING, INTEGER_DECIMAL, AUTO
```

Với dữ liệu sai nhưng kết nối được, thử lần lượt `byte_order`/`word_order` theo bảng sau:

| Thứ tự thường gọi | Cấu hình |
|---|---|
| ABCD | `byte_order: BIG_ENDIAN`, `word_order: BIG_ENDIAN` |
| BADC | `byte_order: LITTLE_ENDIAN`, `word_order: BIG_ENDIAN` |
| CDAB | `byte_order: BIG_ENDIAN`, `word_order: LITTLE_ENDIAN` |
| DCBA | `byte_order: LITTLE_ENDIAN`, `word_order: LITTLE_ENDIAN` |

### 5.3. Ca kiểm thử tối thiểu

1. Đúng IP/cổng/Unit ID: trạng thái `ONLINE`, giá trị đúng.
2. Sai IP hoặc tắt simulator: sau khoảng 60 giây lỗi liên tục sẽ có event `OFFLINE`.
3. Sai `function_code`: thiết bị trả exception hoặc không đọc được.
4. Đổi word order: kết nối vẫn thành công nhưng giá trị phải thay đổi; chọn cấu hình khớp giá trị chuẩn.
5. Bật hai data slot ở hai vùng register khác nhau: cả `data_1` và `data_2` phải có đúng tên và giá trị.

## 6. Test Modbus RTU

### 6.1. Cấu hình mẫu

```json
{
  "name": "CAN-MODBUS-RTU",
  "location_id": 1,
  "manufacturer_id": null,
  "protocol_id": 2,
  "model": "MODBUS-RTU-TEST",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "MODBUS_RTU",
  "poll_interval": 1000,
  "conn_params": {
    "com_port": "COM3",
    "baud_rate": 9600,
    "data_bits": 8,
    "stop_bits": 1,
    "parity": "NONE",
    "unit_id": 1,
    "response_timeout_ms": 3000
  },
  "data_1": {
    "name": "Weight",
    "is_used": true,
    "function_code": 3,
    "start_registers": 0,
    "num_registers": 2,
    "data_type": "FLOAT32",
    "byte_order": "BIG_ENDIAN",
    "word_order": "BIG_ENDIAN",
    "decimal_places": 2
  },
  "data_2": { "is_used": false },
  "data_3": { "is_used": false },
  "data_4": { "is_used": false },
  "data_5": { "is_used": false }
}
```

### 6.2. Điểm cần kiểm tra

- Cổng serial, baud rate, data bits, stop bits và parity phải khớp tuyệt đối.
- `unit_id` bắt buộc trong khoảng 1-247 khi lưu cấu hình.
- Quy tắc register, function code, data type và byte/word order giống Modbus TCP.
- Các cân RTU có cùng `com_port`, `baud_rate`, `data_bits`, `stop_bits`, `parity` được gom vào một bus và đọc tuần tự theo từng `unit_id`.
- Không tạo hai cổng serial vật lý giống nhau cho hai tiến trình ứng dụng khác nhau.

### 6.3. Ca kiểm thử tối thiểu

1. Một slave trên bus: đọc đúng giá trị.
2. Hai slave cùng COM nhưng Unit ID khác nhau: cả hai cân đều cập nhật, không báo lỗi cổng đang bị chiếm.
3. Một Unit ID sai: cân sai ID thất bại nhưng cân còn lại trên cùng bus vẫn tiếp tục cập nhật.
4. Rút cáp/chặn phản hồi: sau khoảng 60 giây lỗi liên tục cân chuyển `OFFLINE`.
5. Sai baud/parity: không có dữ liệu hợp lệ; sửa lại cấu hình phải tự hot-reload mà không cần restart backend.

## 7. Test S-Bus dạng frame (`SBUS`)

Đây là engine S-Bus legacy dùng frame STX/ETX hoặc binary, khác với SABUS ASCII ở mục 8.

### 7.1. Cấu hình mẫu

```json
{
  "name": "CAN-SBUS",
  "location_id": 1,
  "manufacturer_id": null,
  "protocol_id": 3,
  "model": "SBUS-TEST",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "SBUS",
  "poll_interval": 1000,
  "conn_params": {
    "com_port": "COM4",
    "baud_rate": 9600,
    "device_id": 1
  },
  "data_1": {
    "name": "Weight",
    "is_used": true,
    "command": "READ_WEIGHT",
    "payload": "",
    "mode": "ASCII",
    "response_type": "FLOAT32",
    "byte_order": "BIG_ENDIAN",
    "word_order": "LITTLE_ENDIAN"
  },
  "data_2": { "is_used": false },
  "data_3": { "is_used": false },
  "data_4": { "is_used": false },
  "data_5": { "is_used": false }
}
```

Có thể dùng `opcode` dạng hex thay cho `command`, ví dụ `"opcode": "0x57"`. Các command được ánh xạ sẵn:

| Command | Opcode |
|---|---|
| `READ_WEIGHT` hoặc `WEIGHT` | `0x57` |
| `ZERO` | `0x5A` |
| `TARE` | `0x54` |

`mode` hỗ trợ `ASCII` hoặc binary. `response_type` hỗ trợ số signed/unsigned 16, 32, 64-bit và `FLOAT32`/`FLOAT64`.

S-Bus dùng cùng quy ước thứ tự byte/thanh ghi với Modbus:

| Layout thiết bị | Cấu hình |
|---|---|
| `ABCD` | `byte_order: BIG_ENDIAN`, `word_order: BIG_ENDIAN` |
| `BADC` | `byte_order: LITTLE_ENDIAN`, `word_order: BIG_ENDIAN` |
| `CDAB` | `byte_order: BIG_ENDIAN`, `word_order: LITTLE_ENDIAN` |
| `DCBA` | `byte_order: LITTLE_ENDIAN`, `word_order: LITTLE_ENDIAN` |

Có thể khai báo trực tiếp alias trong `byte_order`, ví dụ `"byte_order": "CDAB"`. Nếu không khai báo, engine giữ hành vi cũ là `BIG_ENDIAN/BIG_ENDIAN`.

### 7.2. Lưu ý khi đánh giá

- Engine này đang cố định serial ở 8 data bits, 1 stop bit, no parity và timeout đọc 2000 ms.
- Định dạng frame phải đúng với implementation hiện tại; không nên suy ra chỉ từ tên “S-Bus”. Cần đối chiếu tài liệu thiết bị với frame trong backend.
- Một data field lỗi có thể trả `null` nhưng event vẫn mang trạng thái `ONLINE`. Vì vậy không chỉ nhìn `status`; phải xác nhận `dataValues` có giá trị đúng.
- Engine legacy chưa phát event `OFFLINE` theo ngưỡng 60 giây như Modbus/SABUS/S7.

### 7.3. Ca kiểm thử tối thiểu

1. Lệnh `READ_WEIGHT` ở ASCII trả đúng giá trị.
2. Cấu hình bằng `opcode: 0x57` cho kết quả tương đương.
3. Test binary với đúng `response_length` theo thiết bị.
4. Test giá trị 32-bit với cả word cao trước và word thấp trước.
5. Sai Device ID hoặc opcode: giá trị phải rỗng/null và log phải có lỗi đọc command.

## 8. Test SABUS ASCII (`SABUS`)

Giao thức này gửi lệnh dạng `!{deviceId}FL\r` để đọc lưu lượng tức thời và `!{deviceId}NT\r` để đọc tổng tích lũy.

### 8.1. Cấu hình mẫu

```json
{
  "name": "CAN-SABUS-ASCII",
  "location_id": 1,
  "manufacturer_id": null,
  "protocol_id": 5,
  "model": "SABUS-ASCII-TEST",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "SABUS",
  "poll_interval": 1000,
  "conn_params": {
    "com_port": "COM5",
    "baud_rate": 9600,
    "data_bits": 8,
    "stop_bits": 1,
    "parity": "NONE",
    "device_id": "07",
    "device_id_width": 2,
    "response_timeout_ms": 3000,
    "reconnect_delay_ms": 5000,
    "offline_threshold_ms": 60000,
    "command_prefix": "!",
    "command_terminator": "\\r"
  },
  "data_1": {
    "name": "Instantaneous Flow",
    "command": "FL",
    "is_used": true,
    "decimal_places": 2
  },
  "data_2": {
    "name": "Accumulated Total",
    "command": "NT",
    "is_used": true,
    "decimal_places": 2
  },
  "data_3": { "is_used": false },
  "data_4": { "is_used": false },
  "data_5": { "is_used": false }
}
```

Với cấu hình trên, request thực tế phải là:

```text
!07FL<CR>
!07NT<CR>
```

Engine nhận các response thông dụng như:

```text
!07FL=123.45 kg/h
!07NT:9876,5 kg
```

Nếu response của thiết bị khác chuẩn trên, có thể tách số bằng regex có một capture group:

```json
{
  "name": "Instantaneous Flow",
  "command": "FL",
  "is_used": true,
  "response_pattern": "value=([+-]?[0-9.]+)"
}
```

### 8.2. Điểm cần kiểm tra

- Chỉ chấp nhận command `FL` hoặc `NT` cho data field đang bật.
- `device_id` là bắt buộc; `device_id_width: 2` sẽ biến ID `7` thành `07`.
- `command_terminator` trong JSON là `"\\r"`, được engine giải mã thành ký tự CR thật.
- Các cân SABUS có cùng cấu hình bus serial được gom vào một worker và đọc tuần tự theo station/device ID.
- Response chứa `NAK`, `ERROR` hoặc `ERR` được xem là lỗi.
- Dấu phẩy thập phân đơn, ví dụ `9876,5`, được chuyển thành `9876.5`.

### 8.3. Ca kiểm thử tối thiểu

1. `FL` và `NT` đều trả dữ liệu, đúng tên field.
2. Device ID `7` với width 2 phải phát lệnh chứa `07`.
3. Thiết bị echo lại request trước response: hệ thống vẫn lấy dòng response tiếp theo.
4. Response tùy biến: `response_pattern` lấy đúng nhóm số.
5. Response lỗi/timeout liên tục: sau `offline_threshold_ms`, trạng thái chuyển `OFFLINE`.
6. Hai station chung COM: cả hai cân cập nhật tuần tự và không tranh chấp cổng.

## 9. Test Siemens S7

### 9.1. Cấu hình mẫu

```json
{
  "name": "CAN-SIEMENS-S7",
  "location_id": 1,
  "manufacturer_id": null,
  "protocol_id": 4,
  "model": "S7-1200",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "SIEMENS_S7",
  "poll_interval": 1000,
  "conn_params": {
    "ip": "192.168.1.20",
    "port": 102,
    "rack": 0,
    "slot": 1,
    "controller_type": "S7_1200",
    "connect_timeout_ms": 5000,
    "read_timeout_ms": 5000
  },
  "data_1": {
    "name": "Weight",
    "tag_address": "%DB1:0:REAL",
    "is_used": true,
    "decimal_places": 2
  },
  "data_2": { "is_used": false },
  "data_3": { "is_used": false },
  "data_4": { "is_used": false },
  "data_5": { "is_used": false }
}
```

### 9.2. Điểm cần kiểm tra

- PLC phải bật quyền truy cập phù hợp cho S7 communication từ client ngoài.
- `ip` bắt buộc; `port` mặc định 102, `rack` mặc định 0, `slot` mặc định 1.
- `tag_address` bắt buộc cho mỗi data field đang bật và phải đúng cú pháp PLC4X, ví dụ `%DB1:0:REAL`.
- Mỗi tag phải trả đúng một giá trị; response code phải là `OK`.
- Có thể khai báo `connection_string` đầy đủ trong `conn_params`; nếu có, engine ưu tiên chuỗi này thay vì tự ghép từ IP/rack/slot. Tuy vậy cấu hình vẫn phải có `ip` để vượt qua validation hiện tại.
- Có thể dùng `controller_type` như `S7_300`, `S7_1200` tùy PLC.

### 9.3. Ca kiểm thử tối thiểu

1. Đúng IP/rack/slot/tag: trạng thái `ONLINE`, giá trị bằng giá trị đang monitor trên PLC.
2. Sai tag: log nêu tag trả lỗi; không được ghi nhận một giá trị giả.
3. Sai rack/slot hoặc tắt PLC: engine reconnect và sau khoảng 60 giây phát `OFFLINE`.
4. Bật nhiều tag ở `data_1` đến `data_5`: mọi tag đều có đúng tên và giá trị.

## 10. Test Serial ASCII tổng quát (`SERIAL`)

Cấu hình request/response mẫu, gửi lệnh `W\r` và lấy số từ phản hồi như `ST,GS,+00123.45 kg\r`:

```json
{
  "protocol": "SERIAL",
  "poll_interval": 1000,
  "conn_params": {
    "com_port": "COM5",
    "baud_rate": 9600,
    "data_bits": 8,
    "stop_bits": 1,
    "parity": "NONE",
    "charset": "US-ASCII",
    "command_terminator": "\\r",
    "response_terminator": "\\r",
    "response_timeout_ms": 3000,
    "strip_echo": true
  },
  "data_1": {
    "name": "Weight",
    "command": "W",
    "response_pattern": "([+-]?[0-9]+(?:[.,][0-9]+)?)",
    "value_type": "NUMBER",
    "is_used": true
  }
}
```

Với thiết bị tự phát dữ liệu liên tục, bỏ `command` khỏi data field. Nhiều data field không có `command` sẽ cùng parse một frame nhận được. Có bốn cách lấy giá trị:

- `response_pattern`: regex có ít nhất một capture group; group đầu tiên là giá trị.
- `delimiter` và `value_index`: tách frame thành các cột.
- `start_index` và `length`: lấy vùng ký tự cố định.
- Không khai báo cách parse: tự lấy số đầu tiên trong frame.

Đặt `value_type: "TEXT"` để giữ chuỗi thay vì parse số. Các chuỗi escape `\r`, `\n`, `\t` được hỗ trợ trong command và terminator.

## 11. Biến đổi giá trị dùng chung

Các field numeric của Modbus, SABUS, Serial và Siemens S7 có thể dùng:

```json
{
  "multiplier": 0.1,
  "divisor": 10,
  "offset": 2.5,
  "decimal_places": 2
}
```

Thứ tự xử lý là:

```text
giá trị đọc -> multiplier/scale -> divisor -> offset -> formula -> decimal_places
```

Chỉ cấu hình các phép biến đổi thực sự cần thiết. `divisor` không được bằng 0; `decimal_places` phải từ 0 đến 12. Khi nghiệm thu nên ghi cả giá trị thô trên simulator/PLC và giá trị kỳ vọng sau biến đổi.

## 12. Gửi cấu hình bằng PowerShell

Lưu một JSON mẫu ở trên vào biến `$body`, sau đó gửi:

```powershell
$scaleId = 1
$body = @'
{
  "name": "CAN-MODBUS-TCP",
  "location_id": 1,
  "manufacturer_id": null,
  "protocol_id": 1,
  "model": "MODBUS-TCP-TEST",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "MODBUS_TCP",
  "poll_interval": 1000,
  "conn_params": {
    "ip": "192.168.1.10",
    "port": 502,
    "unit_id": 1
  },
  "data_1": {
    "name": "Weight",
    "is_used": true,
    "function_code": 3,
    "start_registers": 0,
    "num_registers": 2,
    "data_type": "FLOAT32"
  },
  "data_2": { "is_used": false },
  "data_3": { "is_used": false },
  "data_4": { "is_used": false },
  "data_5": { "is_used": false }
}
'@

Invoke-RestMethod `
  -Method Put `
  -Uri "$baseUrl/scales/$scaleId" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $body | ConvertTo-Json -Depth 10
```

Sau khi PUT thành công, gọi lại `GET /scales/{id}` để chắc chắn cấu hình đã được normalize và lưu đúng.

## 13. Test validation khi lưu cấu hình

Các ca âm nên thực hiện cho màn hình/API cấu hình:

| Ca test | Kết quả mong đợi |
|---|---|
| `poll_interval < 100` | HTTP 400 |
| Modbus TCP thiếu `ip` hoặc port ngoài 1-65535 | HTTP 400 |
| Modbus RTU thiếu COM/baud/Unit ID | HTTP 400 |
| Modbus dùng function code khác 3/4 | HTTP 400 |
| Modbus bật field nhưng thiếu start/count | HTTP 400 |
| S7 thiếu IP | HTTP 400 |
| S7 bật field nhưng thiếu `tag_address` | HTTP 400 |
| SABUS thiếu COM/baud/device ID | HTTP 400 |
| SABUS bật field với command khác `FL`/`NT` | HTTP 400 |
| Serial thiếu COM/baud hoặc dùng data bits/stop bits/parity không hợp lệ | HTTP 400 |
| Serial dùng regex không hợp lệ hoặc không có capture group | HTTP 400 |
| `protocol` không được EngineFactory hỗ trợ | Có thể lưu nếu schema không chặn, nhưng engine không khởi động và log báo `Unsupported protocol` |

## 14. Checklist nghiệm thu cho mỗi giao thức

- [ ] Protocol xuất hiện đúng trong danh mục.
- [ ] `protocol_id` và `protocol` cùng chỉ một giao thức.
- [ ] Lưu cấu hình hợp lệ thành công.
- [ ] GET lại cân trả đúng `scale_config`.
- [ ] Engine hot-reload, không cần restart ứng dụng.
- [ ] Kết nối đúng thiết bị/cổng serial.
- [ ] Current state là `ONLINE` và `lastTime` liên tục cập nhật.
- [ ] Tất cả data field đang bật có đúng `name` và `value`.
- [ ] WebSocket nhận event đúng `scaleId`.
- [ ] Weighing history có bản ghi mới.
- [ ] Giá trị khớp thiết bị/simulator, bao gồm byte/word order và hệ số chuyển đổi.
- [ ] Ngắt kết nối tạo hành vi lỗi/offline đúng kỳ vọng của engine.
- [ ] Kết nối lại thì dữ liệu tự phục hồi.
- [ ] Cấu hình sai bị validation từ chối hoặc có log lỗi rõ ràng.

## 15. Xử lý lỗi nhanh

| Hiện tượng | Kiểm tra |
|---|---|
| Lưu được nhưng không có engine | `is_active`, giá trị `protocol`, log `EngineManager` |
| Màn hình hiện protocol A nhưng log chạy protocol B | `protocol_id` đang lệch với `protocol` |
| `ONLINE` nhưng giá trị null ở SBUS | Command/opcode/frame response; không chỉ dựa vào status |
| Modbus timeout | IP/COM, Unit ID, function code, baud/parity, firewall |
| Modbus có số nhưng sai rất lớn/nhỏ | Offset register, `data_type`, byte order, word order |
| Serial báo không mở được cổng | Sai tên cổng, thiếu quyền, cổng đang bị ứng dụng khác giữ |
| SABUS không parse được response | Prefix/ID/terminator hoặc thêm `response_pattern` |
| S7 kết nối được nhưng tag lỗi | Cú pháp tag, DB number/offset/type, quyền PLC |
| Không có history ngay | Chờ batch persistence, kiểm tra current state/WebSocket trước |
| Chỉ một trong nhiều cân RTU/SABUS lỗi | Kiểm tra riêng Unit ID/device ID; bus dùng chung vẫn phải đọc các cân khác |

## 16. Chạy unit test không cần thiết bị thật

Các test này xác nhận validation/normalize cấu hình, chuyển đổi thanh ghi Modbus, gom bus RTU, tạo và parse lệnh SABUS/Serial, và tạo connection string Siemens S7:

```powershell
mvn '-Dtest=ScaleConfigSchemaTest,ModbusRtuEngineTest,SbusEngineTest,SabusAsciiEngineTest,SerialEngineTest,SiemensS7EngineTest,ModbusDataConverterTest' test
```

Kết quả đạt khi Maven báo `BUILD SUCCESS` và không có failure/error. Unit test không thay thế test tích hợp với cân, gateway hoặc PLC thật; nó không xác nhận dây nối, firewall, serial driver hay register/tag của thiết bị.

