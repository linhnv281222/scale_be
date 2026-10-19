# Scale Management API - Update Summary

## Overview
Scale Management APIs đã được cập nhật để gộp chung việc tạo/cập nhật thông tin cân và cấu hình cân vào một request duy nhất. Không cần phải chia làm hai giai đoạn nữa (tạo cân rồi mới cấu hình).

## Changes Made

### 1. Entity Updates

#### Scale.java
- **Thêm field mới**: `Protocol protocol` với annotation `@ManyToOne`
- **Index mới**: `idx_scales_protocol` cho performance
- Tích hợp protocol reference trực tiếp vào Scale entity

### 2. DTO Updates

#### ScaleDto.Request
**Merged fields** - Bao gồm cả thông tin cân và cấu hình:
- **Thông tin cơ bản cân**:
  - `name` - Tên cân
  - `location_id` - ID địa điểm
  - `manufacturer_id` - ID hãng sản xuất
  - `protocol_id` - ID giao thức (MỚI)
  - `model` - Model cân
  - `direction` - Hướng (IMPORT/EXPORT)
  - `is_active` - Trạng thái

- **Cấu hình cân** (tích hợp sẵn):
  - `protocol` - Loại giao thức (MODBUS_TCP, etc.)
  - `poll_interval` - Chu kỳ lấy dữ liệu (ms)
  - `conn_params` - Thông số kết nối (Map)
  - `data_1` - Cấu hình data field 1 (Map)
  - `data_2` - Cấu hình data field 2 (Map)
  - `data_3` - Cấu hình data field 3 (Map)
  - `data_4` - Cấu hình data field 4 (Map)
  - `data_5` - Cấu hình data field 5 (Map)

#### ScaleDto.Response
**Thêm protocol info**:
- `protocol_id` - ID giao thức
- `protocol_name` - Tên giao thức
- `protocol_code` - Mã giao thức
- `scale_config` - Thông tin cấu hình đầy đủ

### 3. Service Layer Updates

#### ScaleService Interface
Simplified từ 14 methods xuống còn **5 methods chính**:

1. `getAllScales(PageRequestDto, Map<String, String>)` - Lấy danh sách có phân trang và filter
2. `getScaleById(Long)` - Lấy theo ID
3. `createScale(ScaleDto.Request)` - Tạo mới cân với cấu hình
4. `updateScale(Long, ScaleDto.Request)` - Cập nhật cân với cấu hình
5. `deleteScale(Long)` - Xóa cân

**Removed methods**:
- `getAllScales()` - Non-paginated list
- `getScalesByLocation(Long)`
- `getScalesByLocations(List<Long>)`
- `getScalesByManufacturer(Long)`
- `getScalesByDirection(String)`
- `getActiveScales()`
- `getScaleConfig(Long)` - Separate config endpoint
- `updateScaleConfig(Long, ScaleConfigDto.Request)` - Separate config update

#### ScaleServiceImpl
**Key changes**:
1. **Protocol validation**: Thêm validation cho protocol khi tạo/cập nhật
2. **Merged config handling**: 
   - `createScaleConfig()` - Tạo config từ request data
   - `updateScaleConfig()` - Cập nhật config từ request data
3. **@Param annotations**: Thêm `@Param("id")` cho cache key resolution
4. **Cache optimization**: Removed separate `scaleConfig` cache, simplified to just `scales` cache

### 4. Controller Updates

#### ScaleController
Simplified từ 10+ endpoints xuống còn **5 core APIs**:

#### **GET /scales**
- **Mục đích**: Lấy danh sách cân với phân trang và filter
- **Search**: `search` - Tìm kiếm trên name, model
- **Filters**: 
  - `locationId` - Lọc theo địa điểm
  - `manufacturerId` - Lọc theo hãng
  - `protocolId` - Lọc theo giao thức
  - `direction` - Lọc theo hướng (IMPORT/EXPORT)
  - `isActive` - Lọc theo trạng thái
- **Pagination**: `page`, `size`, `sort`
- **Response**: `PageResponseDto<ScaleDto.Response>`

#### **GET /scales/{id}**
- **Mục đích**: Lấy thông tin chi tiết 1 cân
- **Response**: Bao gồm cả thông tin cân và cấu hình đầy đủ

#### **POST /scales**
- **Mục đích**: Tạo mới cân với cấu hình
- **Body**: `ScaleDto.Request` (bao gồm cả config fields)
- **Logic**: 
  1. Validate location, manufacturer, protocol
  2. Tạo Scale entity
  3. Tạo ScaleConfig entity với data từ request
  4. Publish event để invalidate cache

#### **PUT /scales/{id}**
- **Mục đích**: Cập nhật cân và cấu hình
- **Body**: `ScaleDto.Request` (bao gồm cả config fields)
- **Logic**:
  1. Validate existing scale
  2. Validate location, manufacturer, protocol
  3. Cập nhật Scale entity
  4. Cập nhật ScaleConfig entity với data từ request
  5. Publish event để invalidate cache

#### **DELETE /scales/{id}**
- **Mục đích**: Xóa cân
- **Logic**: Cascade delete cả config và current state

**Removed endpoints**:
- `GET /scales/all` - Non-paginated
- `GET /scales/location/{locationId}`
- `GET /scales/manufacturer/{manufacturerId}`
- `GET /scales/direction/{direction}`
- `GET /scales/active`
- `GET /scales/{id}/config` - Separate config get
- `PUT /scales/{id}/config` - Separate config update

### 5. Swagger Documentation

**@Tag**: "Scale Management" - APIs for managing scales with integrated configuration

**@Operation annotations**:
- Mỗi endpoint đều có summary và description chi tiết
- Search và filter parameters được document rõ ràng
- Request body có explicit schema với `@io.swagger.v3.oas.annotations.parameters.RequestBody`

**@Schema**: Request DTO có đầy đủ schema cho Swagger UI testing

### 6. Database Migration

**File**: `V1_0_8__add_protocol_to_scales.sql`
- Thêm column `protocol_id BIGINT` vào table `scales`
- Thêm foreign key constraint `fk_scales_protocol`
- Thêm index `idx_scales_protocol`

### 7. Mapper Updates

#### ScaleMapper.java
**toResponseDto()** updated:
- Map protocol info: `protocol_id`, `protocol_name`, `protocol_code`
- Preserve existing config mapping

## API Usage Examples

### Create Scale với Configuration
```json
POST /scales
{
  "name": "Cân Kho 1",
  "location_id": 1,
  "manufacturer_id": 2,
  "protocol_id": 3,
  "model": "XK3190-A12",
  "direction": "IMPORT",
  "is_active": true,
  "protocol": "MODBUS_TCP",
  "poll_interval": 1000,
  "conn_params": {
    "ip": "192.168.1.100",
    "port": 502,
    "slave_id": 1
  },
  "data_1": {
    "name": "Weight",
    "start_registers": 40001,
    "num_registers": 2,
    "is_used": true
  },
  "data_2": {
    "is_used": false
  }
}
```

### Update Scale và Configuration
```json
PUT /scales/1
{
  "name": "Cân Kho 1 - Updated",
  "location_id": 1,
  "manufacturer_id": 2,
  "protocol_id": 3,
  "model": "XK3190-A12+",
  "direction": "EXPORT",
  "is_active": true,
  "protocol": "MODBUS_TCP",
  "poll_interval": 500,
  "conn_params": {
    "ip": "192.168.1.101",
    "port": 502,
    "slave_id": 1
  }
}
```

### Get với Filters
```
GET /scales?page=0&size=20&search=kho&locationId=1&isActive=true&sort=name,asc
```

## Cache Strategy

**Caches used**:
- `scales` - Single cache for all scale operations
  - Key: `#id` for getScaleById
  - Evict: allEntries on create/update/delete

**Removed**:
- `scalesByLocation`
- `scaleConfig`

**@Param annotations**: Tất cả methods với cache key đều có `@Param("id")` để tránh lỗi "Null key returned"

## Benefits

1. **Simplified API**: Từ 10+ endpoints xuống còn 5 core APIs
2. **Better UX**: Không cần 2 bước để tạo cân (tạo → configure)
3. **Consistent with other modules**: Protocol, Manufacturer, Location đều dùng pattern 5 APIs
4. **Full filtering**: GET list support đầy đủ search và filters
5. **Swagger ready**: Đầy đủ documentation để test trên Swagger UI
6. **Cache optimized**: @Param annotations đầy đủ, simplified cache strategy

## Testing Checklist

- [ ] Test POST /scales với full config
- [ ] Test PUT /scales update both scale và config
- [ ] Test GET /scales với search
- [ ] Test GET /scales với filters (locationId, protocolId, direction, isActive)
- [ ] Test GET /scales/{id} trả về đầy đủ config
- [ ] Test DELETE /scales cascade delete config
- [ ] Verify Swagger UI hiển thị đúng schema
- [ ] Verify cache invalidation works
- [ ] Run migration V1_0_8 để thêm protocol_id column

## Notes

- **Default values**: Nếu không truyền config fields, system sẽ tạo default config (MODBUS_TCP, poll_interval=1000)
- **Protocol validation**: protocol_id phải tồn tại trong protocols table
- **Cascade delete**: Xóa scale sẽ tự động xóa config và current_state
- **Event publishing**: Mọi thay đổi đều publish ConfigChangedEvent để hot-reload
