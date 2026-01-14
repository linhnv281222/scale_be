package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.facenet.common.pagination.PageResponseDto;

/**
 * Response DTO for scales with statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ScaleStatsResponse", description = "Response DTO for scales with statistics (total, active, inactive)")
public class ScaleStatsResponseDto {
    
    @Schema(description = "Paginated scale data")
    private PageResponseDto<ScaleDto.Response> data;
    
    @JsonProperty("total_scales")
    @Schema(description = "Total number of scales", example = "100")
    private Long totalScales;
    
    @JsonProperty("active_scales")
    @Schema(description = "Number of active scales", example = "80")
    private Long activeScales;
    
    @JsonProperty("inactive_scales")
    @Schema(description = "Number of inactive scales", example = "20")
    private Long inactiveScales;
}
