package org.facenet.dto.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTOs for Protocol operations
 */
public class ProtocolDto {

    /**
     * Request DTO for creating a new protocol
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ProtocolRequest", description = "Request DTO for creating/updating a protocol")
    public static class Request {
        
        @NotBlank(message = "Protocol code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        @Schema(description = "Unique protocol code", example = "MODBUS_TCP")
        private String code;

        @NotBlank(message = "Protocol name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        @Schema(description = "Protocol display name", example = "Modbus TCP")
        private String name;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(description = "Protocol description")
        private String description;

        @JsonProperty("connection_type")
        @Size(max = 50, message = "Connection type must not exceed 50 characters")
        @Schema(description = "Connection type", example = "TCP")
        private String connectionType;

        @JsonProperty("default_port")
        @Schema(description = "Default TCP port", example = "502")
        private Integer defaultPort;

        @JsonProperty("default_baud_rate")
        @Schema(description = "Default baud rate for serial", example = "9600")
        private Integer defaultBaudRate;

        @JsonProperty("is_active")
        @Schema(description = "Active status")
        private Boolean isActive;

        @JsonProperty("config_template")
        @Schema(description = "JSON configuration template")
        private String configTemplate;
    }

    /**
     * Response DTO for protocol
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ProtocolResponse", description = "Response DTO for protocol")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        
        @Schema(description = "Protocol ID")
        private Long id;

        @Schema(description = "Protocol code")
        private String code;

        @Schema(description = "Protocol name")
        private String name;

        @Schema(description = "Protocol description")
        private String description;

        @JsonProperty("connection_type")
        @Schema(description = "Connection type")
        private String connectionType;

        @JsonProperty("default_port")
        @Schema(description = "Default TCP port")
        private Integer defaultPort;

        @JsonProperty("default_baud_rate")
        @Schema(description = "Default baud rate")
        private Integer defaultBaudRate;

        @JsonProperty("is_active")
        @Schema(description = "Active status")
        private Boolean isActive;

        @JsonProperty("config_template")
        @Schema(description = "Configuration template")
        private String configTemplate;

        @JsonProperty("created_at")
        @Schema(description = "Created timestamp")
        private OffsetDateTime createdAt;

        @JsonProperty("created_by")
        @Schema(description = "Created by")
        private String createdBy;

        @JsonProperty("updated_at")
        @Schema(description = "Updated timestamp")
        private OffsetDateTime updatedAt;

        @JsonProperty("updated_by")
        @Schema(description = "Updated by")
        private String updatedBy;
    }

    /**
     * Simple protocol DTO for dropdown/lookup
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ProtocolSimple", description = "Simple protocol DTO")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Simple {
        
        @Schema(description = "Protocol ID")
        private Long id;

        @Schema(description = "Protocol code")
        private String code;

        @Schema(description = "Protocol name")
        private String name;

        @JsonProperty("connection_type")
        @Schema(description = "Connection type")
        private String connectionType;
    }
}
