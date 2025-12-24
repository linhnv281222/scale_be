package org.facenet.dto.rbac;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTOs for User operations
 */
public class UserDto {

    /**
     * Request DTO for creating a new user
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @Size(max = 100, message = "Full name must not exceed 100 characters")
        private String fullName;

        private Short status;

        @NotEmpty(message = "At least one role is required")
        private List<Integer> roleIds;
    }

    /**
     * Request DTO for updating a user
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        private String fullName;

        private Short status;

        private List<Integer> roleIds;
    }

    /**
     * Response DTO for user with nested roles and permissions (3 levels)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private Long id;
        private String username;
        private String fullName;
        private Short status;
        private List<RoleWithPermissions> roles;
        private OffsetDateTime createdAt;
        private String createdBy;
        private OffsetDateTime updatedAt;
        private String updatedBy;
    }

    /**
     * Nested DTO: Role with its permissions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleWithPermissions {
        private Integer id;
        private String name;
        private String code;
        private List<PermissionDto.Response> permissions;
    }

    /**
     * Simple user DTO for list views
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Simple {
        private Long id;
        private String username;
        private String fullName;
        private Short status;
    }
}
