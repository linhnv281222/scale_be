package org.facenet.dto.manufacturer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTOs for ScaleManufacturer operations
 */
public class ScaleManufacturerDto {

    /**
     * Request DTO for creating/updating a scale manufacturer
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        private String code;

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        private String name;

        @Size(max = 100, message = "Country must not exceed 100 characters")
        private String country;

        @Size(max = 255, message = "Website must not exceed 255 characters")
        private String website;

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        private String phone;

        @Email(message = "Email should be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        private String email;

        @Size(max = 500, message = "Address must not exceed 500 characters")
        private String address;

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;

        @JsonProperty("is_active")
        private Boolean isActive;
    }

    /**
     * Response DTO for scale manufacturer
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private Long id;

        private String code;

        private String name;

        private String country;

        private String website;

        private String phone;

        private String email;

        private String address;

        private String description;

        @JsonProperty("is_active")
        private Boolean isActive;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("created_by")
        private String createdBy;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @JsonProperty("updated_by")
        private String updatedBy;
    }

    /**
     * Simple manufacturer DTO for dropdown/lookup
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
        private String country;
    }
}
