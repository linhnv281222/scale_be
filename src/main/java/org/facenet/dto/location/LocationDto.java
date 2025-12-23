package org.facenet.dto.location;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTOs for Location operations
 */
public class LocationDto {

    /**
     * Request DTO for creating/updating a location
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LocationCreateUpdateRequest", description = "Request payload để tạo hoặc cập nhật vị trí")
    public static class Request {
        @Schema(description = "Mã vị trí (unique)", example = "WS_01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Location code is required")
        @Size(max = 20, message = "Code must not exceed 20 characters")
        private String code;

        @Schema(description = "Tên vị trí", example = "Xưởng A")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        private String name;

        @Schema(description = "ID của vị trí cha (null nếu là level 0)", example = "null")
        @JsonProperty("parent_id")
        private Long parentId;
    }

    /**
     * Response DTO for location with hierarchy
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "LocationResponse", description = "Response trả về thông tin vị trí")
    public static class Response {
        @Schema(description = "ID vị trí", example = "1")
        private Long id;

        @Schema(description = "Mã vị trí", example = "WS_01")
        private String code;

        @Schema(description = "Tên vị trí", example = "Xưởng A")
        private String name;

        @Schema(description = "ID của vị trí cha", example = "null")
        @JsonProperty("parent_id")
        private Long parentId;

        @Schema(description = "Danh sách vị trí con (chỉ có trong tree view)")
        private List<Response> children;

        @Schema(description = "Thời gian tạo", example = "2025-12-23T11:00:00+07:00")
        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @Schema(description = "Người tạo (username)", example = "admin_user")
        @JsonProperty("created_by")
        private String createdBy;

        @Schema(description = "Thời gian cập nhật cuối", example = "2025-12-23T11:00:00+07:00")
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @Schema(description = "Người cập nhật cuối (username)", example = "admin_user")
        @JsonProperty("updated_by")
        private String updatedBy;
    }

    /**
     * Simple location DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Simple {
        private Long id;
        private String code;
        private String name;
    }
}
