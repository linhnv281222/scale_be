# Build ScaleHubIOT - Quick Guide

## 🚀 Cách nhanh nhất (Khuyến nghị)

```powershell
.\build-app.ps1
```

Tạo package hoàn chỉnh trong `package-output\`:
- ✅ Bundled JRE (không cần cài Java)
- ✅ H2 Database (không cần PostgreSQL)
- ✅ Chạy được trên Windows 10/11
- ✅ Kích thước: ~160MB

## 📁 Cấu trúc package-output

```
package-output/
├── ScaleHubIOT.bat        ← Chạy file này
├── ScaleHubIOT.ps1        ← Hoặc file này
├── start-background.bat   ← Chạy nền
├── stop.bat              ← Dừng server
├── README.txt            ← Hướng dẫn
├── bin/
│   └── ScaleHubIOT.jar
├── config/
│   └── application.properties
├── data/                  ← Database
├── logs/                  ← Log files
└── jre/                   ← Java Runtime
```

## 🎯 Các tùy chọn

### 1. Portable Package (Mặc định)
```powershell
.\build-app.ps1
# hoặc
.\build-app.ps1 -Method portable
```

### 2. Launch4j EXE (nhẹ hơn)
```powershell
.\build-app.ps1 -Method launch4j
```
Yêu cầu: Launch4j installed

### 3. Skip rebuild
```powershell
.\build-app.ps1 -SkipBuild
```

## ▶️ Chạy thử

```powershell
cd package-output
.\ScaleHubIOT.bat
```

Sau 10-20 giây:
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html
- H2 Console: http://localhost:8080/api/v1/h2-console

## 📦 Phân phối

1. Zip thư mục `package-output`
2. Giải nén trên máy đích
3. Chạy `ScaleHubIOT.bat`
4. Done! ✨

## ⚙️ Cấu hình

Sửa `config/application.properties`:
- Đổi port: `server.port=8080`
- Đổi database: Xem PostgreSQL section bên dưới
- Logging: `logging.level.org.facenet=DEBUG`

## 🗄️ Sử dụng PostgreSQL

Sửa `config/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scalehub_db
spring.datasource.username=admin
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 🛠️ Development

Chạy với H2 nhanh:
```powershell
.\run-h2.ps1
```

Kiểm tra status:
```powershell
.\check-status.ps1
```

## ℹ️ Chi tiết kỹ thuật

**Script:** `build-app.ps1`

**Methods:**
- `portable` - Tạo package với bundled JRE (~160MB)
- `launch4j` - Tạo single EXE (~90MB, cần Java trên máy đích)

**JRE Bundling:**
- Sử dụng `jlink` để tạo minimal JRE
- Chỉ include modules cần thiết
- Tiết kiệm ~50% dung lượng so với full JRE

**Database:**
- Mặc định: H2 embedded (không cần setup)
- Production: PostgreSQL (sửa config)

---

*Khuyến nghị: Dùng portable package để dễ deploy và không phụ thuộc Java trên máy đích*

