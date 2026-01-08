package org.facenet.dto.scale;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for interval statistics report.
 * Supports filtering by scale(s), date range, and grouping by SHIFT/HOUR/DAY/WEEK.
 * Each data_n field can use its own aggregation method; if missing, defaults to ABS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntervalReportRequestDto {

    /**
     * Optional list of scale IDs. If null/empty, report includes all active scales.
     * Note: If locationId, manufacturerId, or direction filters are provided, they will be applied
     * in addition to scaleIds filter.
     */
    private List<Long> scaleIds;

    /**
     * Optional filter by manufacturer ID.
     */
    private Long manufacturerId;

    /**
     * Optional filter by location ID.
     */
    private Long locationId;

    /**
     * Optional filter by scale direction (IMPORT or EXPORT).
     */
    private String direction;

    /**
     * From date (LocalDate). Use this for simple date-based filtering.
     * If both fromDate/toDate and fromTime/toTime are provided, fromTime/toTime takes precedence.
     * At least one pair (fromDate/toDate OR fromTime/toTime) must be provided.
     */
    private LocalDate fromDate;

    /**
     * To date (LocalDate). Use this for simple date-based filtering.
     */
    private LocalDate toDate;

    /**
     * From time (OffsetDateTime). Use this for precise time-based filtering with timezone.
     * If provided along with toTime, this pair takes precedence over fromDate/toDate.
     */
    private OffsetDateTime fromTime;

    /**
     * To time (OffsetDateTime). Use this for precise time-based filtering with timezone.
     */
    private OffsetDateTime toTime;

    @NotNull(message = "Interval is required")
    private TimeInterval interval;

    /**
     * Per-field aggregation methods.
     * Keys: data_1..data_5. Missing/NULL value defaults to ABS.
     */
    private Map<String, AggregationMethod> aggregationByField;

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
