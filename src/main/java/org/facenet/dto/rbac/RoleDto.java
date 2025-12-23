package org.facenet.dto.rbac;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTOs for Role operations
 */
public class RoleDto {

    /**
     * Request DTO for creating/updating a role
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Role name is required")
        private String name;

        @NotBlank(message = "Role code is required")
        private String code;

        @NotEmpty(message = "At least one permission is required")
        private List<Integer> permissionIds;
    }

    /**
     * Response DTO for role with permissions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private Integer id;
        private String name;
        private String code;
        private List<PermissionDto> permissions;
        private OffsetDateTime createdAt;
        private String createdBy;
        private OffsetDateTime updatedAt;
        private String updatedBy;
    }

    /**
     * Simple role DTO without permissions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Simple {
        private Integer id;
        private String name;
        private String code;
    }
}
