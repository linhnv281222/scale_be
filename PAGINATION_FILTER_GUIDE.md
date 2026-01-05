# API Pagination and Filtering Guide

## Overview
Tất cả các API lấy danh sách đều hỗ trợ phân trang và filter động theo các trường của entity.

## Pagination Parameters

### Query Parameters
- `page`: Số trang (0-indexed, mặc định = 0)
- `size`: Số bản ghi trên mỗi trang (mặc định = 10)
- `sort`: Sắp xếp theo trường (format: `field,direction`)
  - Direction: `asc` (tăng dần) hoặc `desc` (giảm dần)
  - Ví dụ: `sort=name,asc` hoặc `sort=createdAt,desc`
- `search`: Tìm kiếm toàn cục trên nhiều trường

### Response Format
```json
{
  "status": "success",
  "message": "Success",
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "total_elements": 100,
    "total_pages": 10,
    "is_first": true,
    "is_last": false,
    "has_next": true,
    "has_previous": false
  }
}
```

## Key Principles

### 1. Single Endpoint with Multiple Filters
All filtering happens through query parameters on the main endpoint. You can combine as many filters as needed in one request.

**Example:**
```bash
GET /scales?locationId=1&manufacturerId=2&direction=IMPORT&isActive=true&page=0&size=10
```

### 2. Nested Property Filtering
You can filter by nested object properties using dot notation.

**Example:**
```bash
GET /scales?location.id=1&manufacturer.id=2
```

### 3. Dynamic Filter Operators
Add suffixes to field names for different comparison operations.

**Examples:**
```bash
GET /scales?name_like=warehouse          # Contains "warehouse"
GET /scales?createdAt_gte=2025-01-01    # Created after date
GET /scales?totalWeight_lt=1000          # Weight less than 1000
```

### Filter Operators

#### 1. Exact Match (Equals)
- Format: `field=value` hoặc `field_eq=value`
- Ví dụ: `isActive=true`, `connectionType_eq=TCP`

#### 2. Like (String Search)
- Format: `field_like=value`
- Tìm kiếm không phân biệt hoa thường
- Ví dụ: `name_like=modbus`, `code_like=RTU`

#### 3. Greater Than
- Format: `field_gt=value`
- Ví dụ: `createdAt_gt=2025-01-01`

#### 4. Less Than
- Format: `field_lt=value`
- Ví dụ: `defaultPort_lt=1000`

#### 5. Greater Than or Equal
- Format: `field_gte=value`
- Ví dụ: `defaultBaudRate_gte=9600`

#### 6. Less Than or Equal
- Format: `field_lte=value`
- Ví dụ: `totalElements_lte=100`

## Examples

### Example 1: Protocol API

#### Get all protocols with pagination
```bash
GET /protocols?page=0&size=20
```

#### Search protocols
```bash
GET /protocols?search=modbus&page=0&size=10
```

#### Filter by connection type
```bash
GET /protocols?connectionType=TCP&page=0&size=10
```

#### Filter by active status and sort by name
```bash
GET /protocols?isActive=true&sort=name,asc&page=0&size=10
```

#### Combined filters
```bash
GET /protocols?connectionType=SERIAL&isActive=true&search=sbus&sort=createdAt,desc&page=0&size=20
```

#### Filter by name using LIKE
```bash
GET /protocols?name_like=mod&page=0&size=10
```

### Example 2: Manufacturer API

#### Get all manufacturers with pagination
```bash
GET /manufacturers?page=0&size=15
```

#### Search manufacturers by name or code
```bash
GET /manufacturers?search=toledo&page=0&size=10
```

#### Filter by country
```bash
GET /manufacturers?country=Vietnam&page=0&size=10
```

#### Filter active manufacturers and sort
```bash
GET /manufacturers?isActive=true&sort=name,asc&page=0&size=10
```

#### Filter by country using LIKE
```bash
GET /manufacturers?country_like=viet&page=0&size=10
```

### Example 3: Scale API - Single Endpoint with Multiple Filters

#### Get all scales with pagination
```bash
GET /scales?page=0&size=20
```

#### Filter by single condition
```bash
# By location
GET /scales?locationId=1&page=0&size=10

# By manufacturer
GET /scales?manufacturerId=2&page=0&size=10

# By direction
GET /scales?direction=IMPORT&page=0&size=10

# By active status
GET /scales?isActive=true&page=0&size=10
```

#### Combine multiple filters
```bash
# Location + Direction
GET /scales?locationId=1&direction=IMPORT&page=0&size=10

# Manufacturer + Direction + Active
GET /scales?manufacturerId=2&direction=EXPORT&isActive=true&page=0&size=10

# Location + Manufacturer + Direction + Active + Search
GET /scales?locationId=1&manufacturerId=2&direction=IMPORT&isActive=true&search=warehouse&page=0&size=10
```

#### Advanced filtering with nested properties
```bash
# Using nested property path
GET /scales?location.id=1&manufacturer.id=2&page=0&size=10

# Using LIKE operator
GET /scales?name_like=cân&model_like=AB&page=0&size=10

# Combining all features
GET /scales?locationId=1&manufacturerId=2&direction=EXPORT&isActive=true&name_like=warehouse&sort=name,asc&page=0&size=20
```

#### Get scales without pagination
```bash
# All scales
GET /scales/all

# With filters
GET /scales/all?locationId=1&direction=IMPORT
GET /scales/all?manufacturerId=2&isActive=true
GET /scales/all?locationId=1&manufacturerId=2&direction=EXPORT&isActive=true
```

## API Endpoints

### Protocol Management
- **GET** `/protocols` - Paginated list with filters
- **GET** `/protocols/all` - All protocols without pagination (for dropdowns)
- **GET** `/protocols/active` - Active protocols only
- **GET** `/protocols/{id}` - Get by ID
- **GET** `/protocols/code/{code}` - Get by code

### Manufacturer Management
- **GET** `/manufacturers` - Paginated list with filters
- **GET** `/manufacturers/all` - All manufacturers without pagination
- **GET** `/manufacturers/active` - Active manufacturers only
- **GET** `/manufacturers/{id}` - Get by ID
- **GET** `/manufacturers/search?q={keyword}` - Search (legacy endpoint)

### Scale Management
- **GET** `/scales` - Main endpoint with pagination and filters
  - Filters: `locationId`, `manufacturerId`, `direction`, `isActive`, `model`, `name`
  - Nested: `location.id`, `manufacturer.id`
  - Example: `/scales?locationId=1&manufacturerId=2&direction=IMPORT&isActive=true&page=0&size=10`
- **GET** `/scales/all` - Get all scales without pagination (supports same filters)
  - Example: `/scales/all?locationId=1&direction=IMPORT`
- **GET** `/scales/{id}` - Get by ID
- **GET** `/scales/{id}/config` - Get scale configuration
- **PUT** `/scales/{id}/config` - Update scale configuration
- **POST** `/scales` - Create scale
- **PUT** `/scales/{id}` - Update scale
- **DELETE** `/scales/{id}` - Delete scale

## Field Reference

### Protocol Entity Fields
- `id` - Long
- `code` - String
- `name` - String
- `description` - String
- `connectionType` - String (TCP, SERIAL, USB)
- `defaultPort` - Integer
- `defaultBaudRate` - Integer
- `isActive` - Boolean
- `createdAt` - DateTime
- `updatedAt` - DateTime

### ScaleManufacturer Entity Fields
- `id` - Long
- `code` - String
- `name` - String
- `country` - String
- `website` - String
- `phone` - String
- `email` - String
- `address` - String
- `description` - String
- `isActive` - Boolean
- `createdAt` - DateTime
- `updatedAt` - DateTime

### Scale Entity Fields
- `id` - Long
- `name` - String
- `locationId` - Long
- `manufacturerId` - Long
- `model` - String
- `direction` - String (IMPORT, EXPORT)
- `isActive` - Boolean
- `createdAt` - DateTime
- `updatedAt` - DateTime

## Best Practices

1. **Use pagination for large datasets**
   - Default page size is 10
   - Consider increasing size for better performance with fewer requests
   - Max recommended size: 100

2. **Combine filters for precise results**
   - Use multiple filter parameters together
   - Example: `isActive=true&direction=IMPORT&manufacturerId=1`

3. **Use search for flexible queries**
   - Search parameter looks across multiple text fields
   - Better for user-driven search functionality

4. **Use /all endpoints for dropdowns**
   - When you need all items without pagination
   - Useful for select boxes, dropdowns, autocomplete

5. **Sort results appropriately**
   - Default sort: by ID ascending
   - Common sorts: `name,asc`, `createdAt,desc`

6. **Handle empty results**
   - Check `total_elements` in response
   - Empty content array means no results found

## Implementation Notes

### Backend Components
1. **PageRequestDto** - Generic pagination request DTO
2. **PageResponseDto** - Generic pagination response wrapper
3. **GenericSpecification** - Dynamic filter specification builder
4. **JpaSpecificationExecutor** - Spring Data JPA interface for dynamic queries

### Filter Processing
- Filters are applied as AND conditions
- Search is applied as OR across specified fields
- Invalid filter fields are ignored (no error thrown)
- Case-insensitive for string comparisons

### Caching
- Paginated endpoints are not cached
- Use `/all` endpoints with caching for frequently accessed static data
