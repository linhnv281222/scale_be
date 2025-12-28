package org.facenet.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for report data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportData {

    private String reportTitle;
    private String reportCode;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime exportTime;
    private String preparedBy;
    private List<ReportRow> rows;
    private ReportSummary summary;
    private Map<String, Object> metadata;
    
    // Column names from scale_configs
    private String data1Name;
    private String data2Name;
    private String data3Name;
    private String data4Name;
    private String data5Name;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportRow {
        private Integer rowNumber;
        private Long scaleId;
        private String scaleCode;
        private String scaleName;
        private String location;
        private String period;  // Time period for time-based grouping (e.g., "2025-12-20 14:00", "2025-12-20")
        private Double data1Total;
        private Double data2Total;
        private Double data3Total;
        private Double data4Total;
        private Double data5Total;
        private Integer recordCount;
        private OffsetDateTime lastTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportSummary {
        private Integer totalScales;
        private Integer totalRecords;
        private Double data1GrandTotal;
        private Double data2GrandTotal;
        private Double data3GrandTotal;
        private Double data4GrandTotal;
        private Double data5GrandTotal;
        private Double data1Average;
        private Double data2Average;
        private Double data3Average;
        private Double data4Average;
        private Double data5Average;
        private Double data1Max;
        private Double data2Max;
        private Double data3Max;
        private Double data4Max;
        private Double data5Max;
    }
}
