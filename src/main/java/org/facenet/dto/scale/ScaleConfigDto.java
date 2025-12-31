package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(name = "ScaleConfigRequest", description = "Request DTO for scale configuration")
    public static class Request {
        @NotBlank(message = "Protocol is required")
        @Schema(description = "Communication protocol (MODBUS_TCP, MODBUS_RTU, etc.)", 
                example = "MODBUS_TCP", required = true)
        private String protocol;

        @Min(value = 100, message = "Poll interval must be at least 100ms")
        @Schema(description = "Polling interval in milliseconds", example = "1000", defaultValue = "1000")
        @JsonProperty("poll_interval")
        private Integer pollInterval;

        @NotNull(message = "Connection parameters are required")
        @Schema(description = "Connection parameters as key-value pairs", 
                example = "{\"ip\": \"192.168.1.10\", \"port\": 502}", required = true)
        @JsonProperty("conn_params")
        private Map<String, Object> connParams;

        @Schema(description = "Data configuration 1", 
                example = "{\"name\": \"Weight\", \"start_registers\": 40001, \"num_registers\": 2, \"is_used\": true}")
        @JsonProperty("data_1")
        private Map<String, Object> data1;
        
        @Schema(description = "Data configuration 2", 
                example = "{\"is_used\": false}")
        @JsonProperty("data_2")
        private Map<String, Object> data2;
        
        @Schema(description = "Data configuration 3", 
                example = "{\"is_used\": false}")
        @JsonProperty("data_3")
        private Map<String, Object> data3;
        
        @Schema(description = "Data configuration 4", 
                example = "{\"is_used\": false}")
        @JsonProperty("data_4")
        private Map<String, Object> data4;
        
        @Schema(description = "Data configuration 5", 
                example = "{\"is_used\": false}")
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
    @Schema(name = "ScaleConfigResponse", description = "Response DTO for scale configuration")
    public static class Response {
        @Schema(description = "Scale ID", example = "1")
        @JsonProperty("scale_id")
        private Long scaleId;
        
        @Schema(description = "Communication protocol", example = "MODBUS_TCP")
        private String protocol;
        
        @Schema(description = "Polling interval in milliseconds", example = "1000")
        @JsonProperty("poll_interval")
        private Integer pollInterval;
        
        @Schema(description = "Connection parameters", 
                example = "{\"ip\": \"192.168.1.10\", \"port\": 502}")
        @JsonProperty("conn_params")
        private Map<String, Object> connParams;
        
        @Schema(description = "Data configuration 1")
        @JsonProperty("data_1")
        private Map<String, Object> data1;
        
        @Schema(description = "Data configuration 2")
        @JsonProperty("data_2")
        private Map<String, Object> data2;
        
        @Schema(description = "Data configuration 3")
        @JsonProperty("data_3")
        private Map<String, Object> data3;
        
        @Schema(description = "Data configuration 4")
        @JsonProperty("data_4")
        private Map<String, Object> data4;
        
        @Schema(description = "Data configuration 5")
        @JsonProperty("data_5")
        private Map<String, Object> data5;

        @Schema(description = "Last updated timestamp", example = "2025-12-30T10:00:00Z")
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @Schema(description = "User who last updated", example = "admin")
        @JsonProperty("updated_by")
        private String updatedBy;
    }
}
