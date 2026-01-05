package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTOs for Scale operations
 */
public class ScaleDto {

    /**
     * Request DTO for creating/updating a scale
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Scale name is required")
        private String name;

        @JsonProperty("location_id")
        private Long locationId;

        @JsonProperty("manufacturer_id")
        private Long manufacturerId;

        private String model;

        private String direction; // IMPORT or EXPORT

        @JsonProperty("is_active")
        private Boolean isActive;
    }

    /**
     * Response DTO for scale with basic info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private Long id;

        private String name;

        @JsonProperty("location_id")
        private Long locationId;

        @JsonProperty("location_name")
        private String locationName;

        @JsonProperty("manufacturer_id")
        private Long manufacturerId;

        @JsonProperty("manufacturer_name")
        private String manufacturerName;

        @JsonProperty("manufacturer_code")
        private String manufacturerCode;

        private String model;

        private String direction; // IMPORT or EXPORT

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

        @JsonProperty("scale_config")
        private ScaleConfigDto.Response scaleConfig;
    }

    /**
     * Response DTO with current state
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WithState {
        private Long id;

        private String name;

        private String model;

        private String direction; // IMPORT or EXPORT

        private Boolean isActive;

        private StateDto currentState;
    }

    /**
     * DTO for scale state data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StateDto {
        private String data1;

        private String data2;

        private String data3;

        private String data4;

        private String data5;

        private String status;

        private OffsetDateTime lastTime;
    }
}
