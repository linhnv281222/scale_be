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
    
    // Direction-based summaries: flat properties for easier template access
    private DirectionSummary importSummaries;      // direction code 1
    private DirectionSummary exportSummaries;      // direction code 2
    private DirectionSummary unknownSummaries;     // direction code 0
    
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
        
        // Start values (from start_values in interval V2)
        private Double data1Start;
        private Double data2Start;
        private Double data3Start;
        private Double data4Start;
        private Double data5Start;
        
        // End values (from end_values in interval V2)
        private Double data1End;
        private Double data2End;
        private Double data3End;
        private Double data4End;
        private Double data5End;
        
        // Aggregated values (from data_values in interval V2)
        private Double data1Total;
        private Double data2Total;
        private Double data3Total;
        private Double data4Total;
        private Double data5Total;
        
        private Integer recordCount;
        private OffsetDateTime lastTime;
        
        // Direction code: 0=unknown, 1=import, 2=export
        private Integer direction;
        
        // Ratio value (e.g., data_1/data_3 = 1.25)
        private Double ratio;
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
    
    /**
     * Summary statistics for a specific direction
     * Direction: 0=Unknown, 1=Import/Nhập, 2=Export/Xuất
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DirectionSummary {
        private Integer directionCode;  // 0, 1, or 2
        private String directionName;   // "Unknown", "Nhập", "Xuất"
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
    }
}
