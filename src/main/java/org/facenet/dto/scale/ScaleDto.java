package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTOs for Scale operations
 */
public class ScaleDto {

    /**
     * Request DTO for creating/updating a scale (including configuration)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ScaleRequest", description = "Request DTO for scale with configuration")
    public static class Request {
        @NotBlank(message = "Scale name is required")
        @Schema(description = "Scale name", example = "Truck Scale 1", required = true)
        private String name;

        @JsonProperty("location_id")
        @Schema(description = "Location ID", example = "1")
        private Long locationId;

        @JsonProperty("manufacturer_id")
        @Schema(description = "Manufacturer ID", example = "1")
        private Long manufacturerId;

        @JsonProperty("protocol_id")
        @Schema(description = "Protocol ID", example = "1")
        private Long protocolId;

        @Schema(description = "Scale model", example = "TS-5000")
        private String model;

        @Schema(description = "Direction: IMPORT or EXPORT", example = "IMPORT")
        private String direction;

        @JsonProperty("is_active")
        @Schema(description = "Active status", example = "true", defaultValue = "true")
        private Boolean isActive;

        // ========== Configuration fields ==========
        
        @NotBlank(message = "Protocol is required")
        @Schema(description = "Communication protocol (MODBUS_TCP, MODBUS_RTU, etc.)", 
                example = "MODBUS_TCP", required = true)
        private String protocol;

        @Min(value = 100, message = "Poll interval must be at least 100ms")
        @Schema(description = "Polling interval in milliseconds", example = "1000", defaultValue = "1000")
        @JsonProperty("poll_interval")
        private Integer pollInterval;

        @Schema(description = "Connection parameters as key-value pairs", 
                example = "{\"ip\": \"192.168.1.10\", \"port\": 502}")
        @JsonProperty("conn_params")
        private Map<String, Object> connParams;

        @Schema(description = "Data configuration 1", 
                example = "{\"name\": \"Weight\", \"start_registers\": 40001, \"num_registers\": 2, \"is_used\": true}")
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
    }

    /**
     * Response DTO for scale with basic info and configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ScaleResponse", description = "Response DTO for scale with configuration")
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

        @JsonProperty("protocol_id")
        private Long protocolId;

        @JsonProperty("protocol_name")
        private String protocolName;

        @JsonProperty("protocol_code")
        private String protocolCode;

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
