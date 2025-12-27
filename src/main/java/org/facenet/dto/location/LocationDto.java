package org.facenet.dto.location;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    public static class Request {
        @Size(max = 20, message = "Code must not exceed 20 characters")
        private String code;

        @Size(max = 100, message = "Name must not exceed 100 characters")
        private String name;

        @Size(max = 255, message = "Description must not exceed 255 characters")
        private String description;

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
    public static class Response {
        private Long id;

        private String code;

        private String name;

        private String description;

        @JsonProperty("parent_id")
        private Long parentId;

        private List<Response> children;

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
