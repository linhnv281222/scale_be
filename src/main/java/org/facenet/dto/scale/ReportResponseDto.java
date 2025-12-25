package org.facenet.dto.scale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for report response to frontend
 * Contains report metadata and data points for charting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {

    /**
     * Report name/title
     */
    private String reportName;

    /**
     * Aggregation method used
     */
    private String method;

    /**
     * Data field reported
     */
    private String dataField;

    /**
     * Time interval used for grouping
     */
    private String interval;

    /**
     * List of data points for charting
     */
    private List<DataPoint> dataPoints;

    /**
     * Data point for time series chart
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        /**
         * Time bucket (formatted as string)
         */
        private String time;

        /**
         * Aggregated value
         */
        private Double value;
    }
}
