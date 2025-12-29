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
     */
    private List<Long> scaleIds;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    /**
     * Optional precise time range. If provided, it will be used for filtering instead of fromDate/toDate.
     * This is mainly for export flows where the request already has OffsetDateTime boundaries.
     */
    private OffsetDateTime fromTime;

    /**
     * Optional precise time range. If provided, it will be used for filtering instead of fromDate/toDate.
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
