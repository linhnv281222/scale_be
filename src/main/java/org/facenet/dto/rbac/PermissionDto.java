package org.facenet.dto.rbac;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTOs for Permission operations
 */
public class PermissionDto {

    /**
     * Request DTO for creating/updating a permission
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PermissionCreateUpdateRequest", description = "Request payload để tạo hoặc cập nhật quyền hạn")
    public static class Request {
        @Schema(description = "Mã quyền hạn (unique)", example = "READ_USER", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Permission code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        private String code;

        @Schema(description = "Mô tả quyền hạn", example = "Quyền đọc thông tin người dùng")
        @Size(max = 200, message = "Description must not exceed 200 characters")
        private String description;
    }

    /**
     * Response DTO for permission
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PermissionResponse", description = "Response payload cho quyền hạn")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        @Schema(description = "ID quyền hạn", example = "1")
        private Integer id;

        @Schema(description = "Mã quyền hạn", example = "READ_USER")
        private String code;

        @Schema(description = "Mô tả quyền hạn", example = "Quyền đọc thông tin người dùng")
        private String description;

        @Schema(description = "Thời gian tạo", example = "2023-12-01T10:00:00Z")
        private OffsetDateTime createdAt;

        @Schema(description = "Người tạo", example = "admin")
        private String createdBy;
    }
}
