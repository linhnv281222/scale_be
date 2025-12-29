package org.facenet.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;

/**
 * DTO for report export request
 * Enhanced with flexible aggregation and filtering options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportExportRequest {

    @NotNull(message = "Export type is required")
    private ReportExportType type;

    private List<Long> scaleIds; // null means all scales

    @NotNull(message = "Start time is required")
    private OffsetDateTime startTime;

    @NotNull(message = "End time is required")
    private OffsetDateTime endTime;

    // ===== ENHANCED FILTERING OPTIONS =====
    
    /**
     * Data fields to include in report (data_1, data_2, data_3, data_4, data_5)
     * If null, includes all data fields
     */
    private List<String> dataFields;
    
    /**
     * Aggregation method: SUM, AVG, MAX, MIN
     * Default: SUM
     */
    @Builder.Default
    private AggregationMethod aggregationMethod = AggregationMethod.SUM;

    /**
     * Optional per-field aggregation methods.
     * Keys: data_1..data_5
     * If provided (non-empty), overrides {@link #aggregationMethod}.
     */
    private Map<String, AggregationMethod> aggregationByField;

    /**
     * When true, export reuses the interval report engine (supports SHIFT and ABS default rules).
     * If false/null, export uses the legacy in-memory aggregation path.
     */
    @Builder.Default
    private Boolean intervalReport = false;
    
    /**
     * Time interval for grouping: HOUR, DAY, WEEK, MONTH, YEAR
     * If null, uses raw data without grouping
     */
    private TimeInterval timeInterval;
    
    /**
     * Filter by location IDs
     */
    private List<Long> locationIds;
    
    /**
     * Filter by scale status (active/inactive)
     */
    private Boolean activeOnly;
    
    // ===== LEGACY FIELDS (for backward compatibility) =====
    
    @Deprecated
    private String method; // "weighing_logs" or "daily_reports" - kept for backward compatibility

    private String reportTitle;

    private String reportCode;

    private String preparedBy;
    
    // ===== ENUMS =====
    
    public enum AggregationMethod {
        SUM,    // Tổng
        AVG,    // Trung bình
        MAX,    // Lớn nhất
        MIN,    // Nhỏ nhất
        COUNT,  // Đếm số lượng
        ABS     // Độ lệch (|MAX - MIN|)
    }
    
    public enum TimeInterval {
        SHIFT,  // Theo ca
        HOUR,   // Theo giờ
        DAY,    // Theo ngày
        WEEK,   // Theo tuần
        MONTH,  // Theo tháng
        YEAR    // Theo năm
    }
}

