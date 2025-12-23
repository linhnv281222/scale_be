# ScaleHub IoT - Weighing Scale Management System

## 📋 Tổng quan
Hệ thống quản lý cân điện tử IoT với kiến trúc monolithic Spring Boot, hỗ trợ kết nối Modbus TCP/RTU/Serial, real-time data streaming qua WebSocket, và RBAC đầy đủ.

## 🚀 Khởi động nhanh

### Yêu cầu
- Java 17+
- Maven 3.6+
- PostgreSQL 12+ (cho production) hoặc H2 (cho development)

### Chạy với H2 Database (Development)
```bash
# Build project
mvn clean install -DskipTests

# Khởi động server
mvn spring-boot:run

# Hoặc chạy từ JAR
java -jar target/ScaleHubIOT-1.0-SNAPSHOT.jar
```

Server sẽ khởi động tại: http://localhost:8080/api/v1

### Chạy với PostgreSQL (Production)

1. **Tạo database**:
```sql
CREATE DATABASE scalehub_db OWNER postgres ENCODING 'UTF8';
```

2. **Cập nhật application.properties**:
```properties
spring.profiles.active=prod
```

3. **Tạo file application-prod.properties**:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scalehub_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

4. **Khởi động**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 📚 API Documentation

### Swagger UI
Truy cập: http://localhost:8080/api/v1/swagger-ui.html

### H2 Console (chỉ khi dùng H2)
Truy cập: http://localhost:8080/api/v1/h2-console
- JDBC URL: `jdbc:h2:mem:scalehub_db`
- Username: `sa`
- Password: (để trống)

## 🔐 Authentication

### Login
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

### Sử dụng Token
```bash
GET /api/v1/users
Authorization: Bearer eyJhbGc...
```

## 🏗️ Kiến trúc

### Package Structure
```
org.facenet
├── common/              # Common utilities, exceptions, responses
│   ├── audit/          # JPA Auditing (createdAt, updatedAt, createdBy, updatedBy)
│   ├── exception/      # Exception handlers
│   └── response/       # API response wrappers
├── config/             # Spring configurations
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   ├── WebSocketConfig.java
│   ├── ActiveQueueConfig.java
│   └── JacksonConfig.java
├── controller/         # REST Controllers
│   ├── auth/          # Authentication endpoints
│   └── rbac/          # User, Role, Permission management
├── dto/               # Data Transfer Objects
│   ├── auth/
│   ├── rbac/
│   └── scale/
├── entity/            # JPA Entities
│   ├── rbac/          # User, Role, Permission
│   └── scale/         # Scale, ScaleConfig, WeighingLog, etc.
├── repository/        # JPA Repositories
├── service/           # Business logic
├── security/          # JWT, UserDetailsService
├── mapper/            # Entity <-> DTO mappers
└── event/             # Application events
```

### Database Schema

#### RBAC Tables
- `users` - Thông tin người dùng
- `roles` - Vai trò hệ thống
- `permissions` - Quyền hạn chi tiết
- `user_roles` - Many-to-many User-Role
- `role_permissions` - Many-to-many Role-Permission

#### Scale Tables
- `locations` - Cấu trúc phân cấp địa điểm (self-referencing)
- `scales` - Thiết bị cân
- `scale_configs` - Cấu hình chi tiết cho từng cân
- `scale_current_state` - Trạng thái real-time (updated by active queue)
- `weighing_logs` - Log lịch sử cân (partitioned by created_at)
- `scale_daily_reports` - Báo cáo tổng hợp theo ngày
- `form_templates` - Templates cho form động

## 🔧 Configuration

### JWT Settings
```properties
jwt.secret=YourSecretKeyHere_MinimumLength256bits
jwt.expiration=86400000          # 24 hours
jwt.refresh-expiration=604800000 # 7 days
```

### Device Engine
```properties
device.engine.worker-threads=8
device.engine.queue-capacity=100000
device.engine.default-poll-interval=1000
device.engine.connection-timeout=5000
```

### Modbus
```properties
modbus.tcp.port=502
modbus.tcp.unit-id=1
modbus.rtu.baud-rate=9600
modbus.rtu.data-bits=8
modbus.rtu.stop-bits=1
modbus.rtu.parity=NONE
```

## 🌐 WebSocket

### Connect
```javascript
const socket = new SockJS('http://localhost:8080/api/v1/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe to scale data
    stompClient.subscribe('/topic/scales/{scaleId}', function(message) {
        const data = JSON.parse(message.body);
        console.log('Scale data:', data);
    });
});
```

## 📊 Active Queue Architecture

Hệ thống sử dụng `ArrayBlockingQueue` với 8 worker threads để xử lý data từ devices:

1. Device polling threads đọc data từ Modbus
2. Data được push vào `BlockingQueue` (capacity: 100,000)
3. Worker threads consume từ queue và:
   - Update `scale_current_state` (real-time state)
   - Insert vào `weighing_logs` (historical data)
   - Broadcast qua WebSocket

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Skip tests during build
mvn clean install -DskipTests
```

## 📝 API Examples

### User Management

#### Get all users
```bash
GET /api/v1/users
Authorization: Bearer {token}
```

#### Get user by ID (with nested roles and permissions)
```bash
GET /api/v1/users/1
Authorization: Bearer {token}

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "fullName": "Administrator",
    "email": "admin@example.com",
    "status": 1,
    "roles": [
      {
        "id": 1,
        "code": "ADMIN",
        "name": "Administrator",
        "permissions": [
          {
            "id": 1,
            "code": "USER_CREATE",
            "name": "Create User",
            "resource": "USER",
            "action": "CREATE"
          }
        ]
      }
    ]
  }
}
```

#### Create user
```bash
POST /api/v1/users
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "john",
  "password": "password123",
  "fullName": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "0123456789",
  "roleIds": [2]
}
```

### Role Management

#### Create role with permissions
```bash
POST /api/v1/roles
Authorization: Bearer {token}
Content-Type: application/json

{
  "code": "OPERATOR",
  "name": "Scale Operator",
  "description": "Can operate scales",
  "permissionIds": [5, 6, 7]
}
```

## 🔍 Troubleshooting

### Lỗi không kết nối PostgreSQL
- Đổi sang profile `dev` để dùng H2: `spring.profiles.active=dev`
- Hoặc đảm bảo PostgreSQL đang chạy và database đã tạo

### Lỗi port 8080 đã dùng
Thay đổi port trong `application.properties`:
```properties
server.port=8081
```

### Lỗi JWT secret
Đảm bảo secret key đủ dài (>= 256 bits):
```properties
jwt.secret=Your_Very_Long_Secret_Key_At_Least_256_Bits_Long_For_HS256_Algorithm
```

## 📦 Build Production

```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/ScaleHubIOT-1.0-SNAPSHOT.jar --spring.profiles.active=prod

# Build Docker image (if Dockerfile exists)
docker build -t scalehub-iot:latest .
docker run -p 8080:8080 scalehub-iot:latest
```

## 📄 License
Proprietary - All Rights Reserved

## 👥 Contact
- Developer: FaceNet Team
- Email: support@facenet.vn
