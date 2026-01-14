package org.facenet.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.facenet.dto.scale.IntervalReportRequestDtoV2;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Interval Report V2 export request
 * Combines parameters from IntervalReportRequestDtoV2 for data generation
 * and template import ID for template selection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntervalReportExportRequestV2 {

    // ===== TEMPLATE SELECTION =====
    
    /**
     * Template import ID to use for export
     */
    @NotNull(message = "Template import ID is required")
    private Long importId;
    
    // ===== INTERVAL REPORT V2 PARAMETERS =====
    
    /**
     * Scale IDs to include in report
     * If null or empty, includes scales based on other filters
     */
    private List<Long> scaleIds;
    
    /**
     * Manufacturer IDs for filtering (supports multiple)
     */
    private List<Long> manufacturerIds;
    
    /**
     * Location IDs for filtering (supports multiple)
     */
    private List<Long> locationIds;
    
    /**
     * Direction filter: IMPORT or EXPORT
     */
    private String direction;
    
    /**
     * Shift IDs for filtering (supports multiple)
     * Only applicable when interval is SHIFT
     */
    private List<Long> shiftIds;
    
    /**
     * Report start time
     */
    @NotNull(message = "From time is required")
    private OffsetDateTime fromTime;
    
    /**
     * Report end time
     */
    @NotNull(message = "To time is required")
    private OffsetDateTime toTime;
    
    /**
     * Time interval for grouping: HOUR, DAY, WEEK, MONTH, YEAR, SHIFT
     */
    @NotNull(message = "Interval is required")
    private IntervalReportRequestDtoV2.TimeInterval interval;
    
    /**
     * Per-field aggregation methods
     * Keys: data_1, data_2, data_3, data_4, data_5
     * Values: SUM, AVG, MAX, MIN
     */
    private Map<String, IntervalReportRequestDtoV2.AggregationMethod> aggregationByField;
    
    /**
     * Ratio calculation formula (e.g., "data_1/data_3")
     * Default: "data_1/data_3"
     */
    @Builder.Default
    private String ratioFormula = "data_1/data_3";
    
    /**
     * Pagination: page number (for API consistency, not used in export)
     */
    @Builder.Default
    private Integer page = 0;
    
    /**
     * Pagination: page size (for API consistency, not used in export)
     */
    @Builder.Default
    private Integer size = 1000;
}
