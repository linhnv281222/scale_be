package org.facenet.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class DashboardDto {
    private DashboardDto() {
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Overview {
        @JsonProperty("generated_at") private OffsetDateTime generatedAt;
        private String timezone;
        @JsonProperty("total_scales") private int totalScales;
        @JsonProperty("online_scales") private int onlineScales;
        @JsonProperty("offline_scales") private int offlineScales;
        @JsonProperty("total_field") private String totalField;
        private PeriodKpi day;
        private PeriodKpi week;
        private PeriodKpi month;
        private List<ShiftProduction> shifts;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PeriodKpi {
        @JsonProperty("period_start") private OffsetDateTime periodStart;
        @JsonProperty("period_end") private OffsetDateTime periodEnd;
        @JsonProperty("previous_start") private OffsetDateTime previousStart;
        @JsonProperty("previous_end") private OffsetDateTime previousEnd;
        @JsonProperty("current_value") private BigDecimal currentValue;
        @JsonProperty("previous_value") private BigDecimal previousValue;
        @JsonProperty("change_percentage") private BigDecimal changePercentage;
        @JsonProperty("achievement_percentage") private BigDecimal achievementPercentage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ShiftProduction {
        @JsonProperty("shift_id") private Long shiftId;
        @JsonProperty("shift_code") private String shiftCode;
        @JsonProperty("shift_name") private String shiftName;
        @JsonProperty("start_time") private LocalTime startTime;
        @JsonProperty("end_time") private LocalTime endTime;
        private BigDecimal production;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScaleTable {
        @JsonProperty("generated_at") private OffsetDateTime generatedAt;
        @JsonProperty("total_field") private String totalField;
        @JsonProperty("flow_field") private String flowField;
        private List<ScaleDetail> scales;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScaleDetail {
        @JsonProperty("scale_id") private Long scaleId;
        @JsonProperty("scale_name") private String scaleName;
        @JsonProperty("location_id") private Long locationId;
        @JsonProperty("location_name") private String locationName;
        private String status;
        @JsonProperty("last_time") private OffsetDateTime lastTime;
        @JsonProperty("current_flow") private BigDecimal currentFlow;
        @JsonProperty("current_total") private BigDecimal currentTotal;
        @JsonProperty("shift_production") private Map<String, BigDecimal> shiftProduction;
        private PeriodKpi day;
        private PeriodKpi week;
        private PeriodKpi month;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Trend {
        private OffsetDateTime from;
        private OffsetDateTime to;
        private String bucket;
        @JsonProperty("flow_field") private String flowField;
        private List<TrendSeries> series;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrendSeries {
        @JsonProperty("scale_id") private Long scaleId;
        @JsonProperty("scale_name") private String scaleName;
        private List<TrendPoint> points;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrendPoint {
        private OffsetDateTime timestamp;
        private BigDecimal value;
    }
}
