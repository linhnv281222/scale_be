package org.facenet.dto.scale;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for interval statistics report V2.
 * Simplified version - uses only one pair of time parameters (fromTime/toTime).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntervalReportRequestDtoV2 {

    /**
     * Optional list of scale IDs. If null/empty, report includes all active scales.
     */
    private List<Long> scaleIds;

    /**
     * Optional filter by manufacturer IDs (supports multiple).
     */
    private List<Long> manufacturerIds;

    /**
     * Optional filter by location IDs (supports multiple).
     */
    private List<Long> locationIds;

    /**
     * Optional filter by scale direction (IMPORT or EXPORT).
     */
    private String direction;

    /**
     * Optional filter by shift IDs (supports multiple).
     */
    private List<Long> shiftIds;

    /**
     * From time (OffsetDateTime) - REQUIRED.
     */
    @NotNull(message = "From time is required")
    private OffsetDateTime fromTime;

    /**
     * To time (OffsetDateTime) - REQUIRED.
     */
    @NotNull(message = "To time is required")
    private OffsetDateTime toTime;

    /**
     * Time interval for grouping (SHIFT/HOUR/DAY/WEEK) - REQUIRED.
     */
    @NotNull(message = "Interval is required")
    private TimeInterval interval;

    /**
     * Per-field aggregation methods.
     * Keys: data_1..data_5. Missing/NULL value defaults to ABS.
     */
    private Map<String, AggregationMethod> aggregationByField;

    /**
     * Ratio calculation configuration.
     * Format: "data_x/data_y" (e.g., "data_1/data_3").
     * Default: "data_1/data_3" if not provided.
     */
    private String ratioFormula;

    /**
     * Page number for pagination (0-based). Default: 0.
     */
    private Integer page;

    /**
     * Page size for pagination. Default: 20. Max: 1000.
     */
    private Integer size;

    public enum TimeInterval {
        SHIFT,
        HOUR,
        DAY,
        WEEK
    }

    public enum AggregationMethod {
        SUM,
        AVG,
        MAX,
        MIN,
        COUNT,
        ABS
    }
}
