package org.facenet.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation
 * 
 * Access Swagger UI at: http://localhost:8080/api/v1/swagger-ui.html
 * Access API docs at: http://localhost:8080/api/v1/api-docs
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "ScaleHub IoT API",
        version = "1.0.0",
        description = """
            ## ScaleHub IoT - Weighing System Management API
            
            Hệ thống quản lý cân công nghiệp với các tính năng:
            
            ### Module 1: RBAC (Role-Based Access Control)
            - **Users**: Quản lý người dùng
            - **Roles**: Quản lý vai trò
            - **Permissions**: Quản lý quyền hạn
            
            ### Module 2: Configuration Management ✨ NEW
            - **Locations**: Quản lý vị trí (hỗ trợ cấu trúc phân cấp)
            - **Scales**: Quản lý thiết bị cân
            - **Scale Configs**: Cấu hình kỹ thuật (protocol, registers, polling interval)
            
            ### Module 3: Real-time & Historical Data (Coming Soon)
            - **Real-time Monitoring**: Theo dõi dữ liệu thời gian thực qua WebSocket
            - **Historical Data**: Lưu trữ và truy vấn lịch sử đo
            - **Reports**: Báo cáo theo ngày, tuần, tháng
            
            ### Authentication
            Hệ thống sử dụng JWT Bearer Token authentication.
            
            **Để sử dụng API:**
            1. Đăng nhập tại `/auth/login` để nhận `access_token`
            2. Click nút **Authorize** ở đầu trang
            3. Nhập: `Bearer {your_access_token}`
            4. Click **Authorize** và bắt đầu test APIs
            
            ### Base URL
            `/api/v1`
            
            ### Quick Start - Module 2
            **Tạo Location:**
            ```
            POST /api/v1/locations
            {"code": "WS_01", "name": "Xưởng A", "parent_id": null}
            ```
            
            **Tạo Scale:**
            ```
            POST /api/v1/scales
            {"name": "Cân 01", "location_id": 1, "model": "IND570", "is_active": true}
            ```
            
            **Cập nhật Config:**
            ```
            PUT /api/v1/scales/{id}/config
            {"protocol": "MODBUS_TCP", "poll_interval": 1000, ...}
            ```
            """,
        contact = @Contact(
            name = "ScaleHub IoT Support",
            email = "support@scalehub.com",
            url = "https://scalehub.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
        )
    ),
    servers = {
        @Server(
            description = "Local Development Server",
            url = "http://localhost:8080/api/v1"
        ),
        @Server(
            description = "Production Server",
            url = "https://api.scalehub.com/api/v1"
        )
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    in = SecuritySchemeIn.HEADER,
    description = "JWT Bearer token authentication. Nhập token nhận được từ /auth/login"
)
public class OpenApiConfig {
}
