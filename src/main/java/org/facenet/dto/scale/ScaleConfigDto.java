package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTOs for ScaleConfig operations
 */
public class ScaleConfigDto {

    /**
     * Request DTO for creating/updating scale configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Protocol is required")
        private String protocol;

        @Min(value = 100, message = "Poll interval must be at least 100ms")
        @JsonProperty("poll_interval")
        private Integer pollInterval;

        @NotNull(message = "Connection parameters are required")
        @JsonProperty("conn_params")
        private Map<String, Object> connParams;

        @JsonProperty("data_1")
        private Map<String, Object> data1;
        
        @JsonProperty("data_2")
        private Map<String, Object> data2;
        
        @JsonProperty("data_3")
        private Map<String, Object> data3;
        
        @JsonProperty("data_4")
        private Map<String, Object> data4;
        
        @JsonProperty("data_5")
        private Map<String, Object> data5;
    }

    /**
     * Response DTO for scale configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        @JsonProperty("scale_id")
        private Long scaleId;
        
        private String protocol;
        
        @JsonProperty("poll_interval")
        private Integer pollInterval;
        
        @JsonProperty("conn_params")
        private Map<String, Object> connParams;
        
        @JsonProperty("data_1")
        private Map<String, Object> data1;
        
        @JsonProperty("data_2")
        private Map<String, Object> data2;
        
        @JsonProperty("data_3")
        private Map<String, Object> data3;
        
        @JsonProperty("data_4")
        private Map<String, Object> data4;
        
        @JsonProperty("data_5")
        private Map<String, Object> data5;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @JsonProperty("updated_by")
        private String updatedBy;
    }
}
