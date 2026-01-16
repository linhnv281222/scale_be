package org.facenet.dto.shift;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating shift result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update shift result")
public class ShiftResultUpdateRequestDto {

    @Schema(description = "Start value of data_1", example = "1500.50")
    private String startValueData1;

    @Schema(description = "End value of data_1", example = "2500.75")
    private String endValueData1;

    @Schema(description = "Deviation of data_1 (will be calculated if not provided)", example = "1000.25")
    private BigDecimal deviationData1;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive;
}
