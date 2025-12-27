package org.facenet.dto.rbac;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    public static class Request {
        @NotBlank(message = "Permission code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        private String code;

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private Integer id;

        private String name;

        private String resource;

        private String action;

        private String description;

        private OffsetDateTime createdAt;

        private String createdBy;
    }
}
