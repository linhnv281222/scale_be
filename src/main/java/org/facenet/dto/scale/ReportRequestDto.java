package org.facenet.dto.scale;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for report request from frontend
 * Contains all parameters needed to generate a report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {

    /**
     * List of scale IDs to include in report
     * User can select multiple scales from UI
     */
    @NotEmpty(message = "Scale IDs cannot be empty")
    private List<Long> scaleIds;

    /**
     * Data field to aggregate (data_1, data_2, data_3, data_4, data_5)
     */
    @NotNull(message = "Data field is required")
    private String dataField;

    /**
     * Aggregation method: SUM, AVG, MAX
     */
    @NotNull(message = "Method is required")
    private AggregationMethod method;

    /**
     * Start date of report range
     */
    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    /**
     * End date of report range
     */
    @NotNull(message = "To date is required")
    private LocalDate toDate;

    /**
     * Time interval for grouping: HOUR, DAY, WEEK, MONTH, YEAR
     */
    @NotNull(message = "Interval is required")
    private TimeInterval interval;

    /**
     * Aggregation methods
     */
    public enum AggregationMethod {
        SUM,
        AVG,
        MAX
    }

    /**
     * Time intervals for grouping
     */
    public enum TimeInterval {
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR
    }
}
