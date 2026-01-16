package org.facenet.dto.shift;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response DTO for shift result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Shift result information")
public class ShiftResultResponseDto {

    @Schema(description = "Shift result ID", example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Scale ID", example = "1")
    @JsonProperty("scale_id")
    private Long scaleId;

    @Schema(description = "Scale name", example = "Cân 01")
    @JsonProperty("scale_name")
    private String scaleName;

    @Schema(description = "Shift ID", example = "1")
    @JsonProperty("shift_id")
    private Long shiftId;

    @Schema(description = "Shift code", example = "CA1")
    @JsonProperty("shift_code")
    private String shiftCode;

    @Schema(description = "Shift name", example = "Ca sáng")
    @JsonProperty("shift_name")
    private String shiftName;

    @Schema(description = "Shift date", example = "2026-01-15")
    @JsonProperty("shift_date")
    private LocalDate shiftDate;

    @Schema(description = "Start value of data_1", example = "1500.50")
    @JsonProperty("start_value_data1")
    private String startValueData1;

    @Schema(description = "End value of data_1", example = "2500.75")
    @JsonProperty("end_value_data1")
    private String endValueData1;

    @Schema(description = "Deviation of data_1 (end - start)", example = "1000.25")
    @JsonProperty("deviation_data1")
    private BigDecimal deviationData1;

    @Schema(description = "Active status", example = "true")
    @JsonProperty("is_active")
    private Boolean isActive;

    @Schema(description = "Created timestamp", example = "2026-01-15T08:00:00+07:00")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @Schema(description = "Created by", example = "admin")
    @JsonProperty("created_by")
    private String createdBy;

    @Schema(description = "Updated timestamp", example = "2026-01-15T16:00:00+07:00")
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Updated by", example = "admin")
    @JsonProperty("updated_by")
    private String updatedBy;
}
