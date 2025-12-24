package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
     * Request DTO for creating/updating a scale
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ScaleCreateUpdateRequest", description = "Request payload để tạo hoặc cập nhật thiết bị cân")
    public static class Request {
        @Schema(description = "Tên thiết bị cân", example = "Cân 01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Scale name is required")
        private String name;

        @Schema(description = "ID vị trí đặt thiết bị", example = "1")
        @JsonProperty("location_id")
        private Long locationId;

        @Schema(description = "Model của thiết bị cân", example = "IND570")
        private String model;

        @Schema(description = "Trạng thái hoạt động", example = "true")
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
    @Schema(name = "ScaleResponse", description = "Response trả về thông tin thiết bị cân")
    public static class Response {
        @Schema(description = "ID thiết bị cân", example = "100")
        private Long id;

        @Schema(description = "Tên thiết bị cân", example = "Cân 01")
        private String name;

        @Schema(description = "ID vị trí đặt thiết bị", example = "1")
        @JsonProperty("location_id")
        private Long locationId;

        @Schema(description = "Tên vị trí đặt thiết bị", example = "Xưởng A")
        @JsonProperty("location_name")
        private String locationName;

        @Schema(description = "Model của thiết bị cân", example = "IND570")
        private String model;

        @Schema(description = "Trạng thái hoạt động", example = "true")
        @JsonProperty("is_active")
        private Boolean isActive;

        @Schema(description = "Thời gian tạo", example = "2025-12-23T11:05:00+07:00")
        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @Schema(description = "Người tạo (username)", example = "system")
        @JsonProperty("created_by")
        private String createdBy;

        @Schema(description = "Thời gian cập nhật cuối", example = "2025-12-23T11:05:00+07:00")
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @Schema(description = "Người cập nhật cuối (username)", example = "system")
        @JsonProperty("updated_by")
        private String updatedBy;

        @Schema(description = "Cấu hình kỹ thuật của thiết bị cân")
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
        @Schema(description = "ID thiết bị cân", example = "100")
        private Long id;

        @Schema(description = "Tên thiết bị cân", example = "Cân 01")
        private String name;

        @Schema(description = "Model của thiết bị cân", example = "IND570")
        private String model;

        @Schema(description = "Trạng thái hoạt động", example = "true")
        private Boolean isActive;

        @Schema(description = "Trạng thái hiện tại của thiết bị")
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
        @Schema(description = "Dữ liệu 1")
        private String data1;

        @Schema(description = "Dữ liệu 2")
        private String data2;

        @Schema(description = "Dữ liệu 3")
        private String data3;

        @Schema(description = "Dữ liệu 4")
        private String data4;

        @Schema(description = "Dữ liệu 5")
        private String data5;

        @Schema(description = "Trạng thái kết nối")
        private String status;

        @Schema(description = "Thời gian cập nhật cuối", example = "2025-12-23T11:10:00Z")
        private OffsetDateTime lastTime;
    }
}
