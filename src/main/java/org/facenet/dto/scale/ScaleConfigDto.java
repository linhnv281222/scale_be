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
    @Schema(name = "ScaleConfigUpdateRequest", description = "Request payload để cập nhật cấu hình thiết bị cân")
    public static class Request {
        @Schema(description = "Protocol type", example = "MODBUS_TCP", allowableValues = {"MODBUS_TCP", "MODBUS_RTU", "SERIAL"})
        @NotBlank(message = "Protocol is required")
        private String protocol;

        @Schema(description = "Polling interval in milliseconds", example = "1000", minimum = "100")
        @Min(value = 100, message = "Poll interval must be at least 100ms")
        @JsonProperty("poll_interval")
        private Integer pollInterval;

        @Schema(description = "Connection parameters (JSON)", example = "{\"ip\": \"192.168.1.10\", \"port\": 502}")
        @NotNull(message = "Connection parameters are required")
        @JsonProperty("conn_params")
        private Map<String, Object> connParams;

        @Schema(description = "Data channel 1 configuration", example = "{\"name\": \"Weight\", \"start_registers\": 40001, \"num_registers\": 2, \"is_used\": true}")
        @JsonProperty("data_1")
        private Map<String, Object> data1;
        
        @Schema(description = "Data channel 2 configuration", example = "{\"name\": \"Status\", \"start_registers\": 40003, \"num_registers\": 1, \"is_used\": true}")
        @JsonProperty("data_2")
        private Map<String, Object> data2;
        
        @Schema(description = "Data channel 3 configuration", example = "{\"is_used\": false}")
        @JsonProperty("data_3")
        private Map<String, Object> data3;
        
        @Schema(description = "Data channel 4 configuration", example = "{\"is_used\": false}")
        @JsonProperty("data_4")
        private Map<String, Object> data4;
        
        @Schema(description = "Data channel 5 configuration", example = "{\"is_used\": false}")
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
    @Schema(name = "ScaleConfigResponse", description = "Response trả về cấu hình thiết bị cân")
    public static class Response {
        @Schema(description = "Scale ID", example = "100")
        @JsonProperty("scale_id")
        private Long scaleId;
        
        @Schema(description = "Protocol type", example = "MODBUS_TCP")
        private String protocol;
        
        @Schema(description = "Polling interval in milliseconds", example = "1000")
        @JsonProperty("poll_interval")
        private Integer pollInterval;
        
        @Schema(description = "Connection parameters", example = "{\"ip\": \"192.168.1.10\", \"port\": 502}")
        @JsonProperty("conn_params")
        private Map<String, Object> connParams;
        
        @Schema(description = "Data channel 1", example = "{\"name\": \"Weight\", \"start_registers\": 40001, \"num_registers\": 2, \"is_used\": true}")
        @JsonProperty("data_1")
        private Map<String, Object> data1;
        
        @Schema(description = "Data channel 2", example = "{\"name\": \"Status\", \"start_registers\": 40003, \"num_registers\": 1, \"is_used\": true}")
        @JsonProperty("data_2")
        private Map<String, Object> data2;
        
        @Schema(description = "Data channel 3", example = "{\"is_used\": false}")
        @JsonProperty("data_3")
        private Map<String, Object> data3;
        
        @Schema(description = "Data channel 4", example = "{\"is_used\": false}")
        @JsonProperty("data_4")
        private Map<String, Object> data4;
        
        @Schema(description = "Data channel 5", example = "{\"is_used\": false}")
        @JsonProperty("data_5")
        private Map<String, Object> data5;

        @Schema(description = "Last update timestamp", example = "2025-12-23T11:10:00+07:00")
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @Schema(description = "Last updated by (username)", example = "tech_lead")
        @JsonProperty("updated_by")
        private String updatedBy;
    }
}
