package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for interval statistics report V2.
 * Includes start/end values, ratio calculation, and overview statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntervalReportResponseDtoV2 {

    private IntervalReportRequestDtoV2.TimeInterval interval;
    private String fromDate;
    private String toDate;

    /**
     * Column display names resolved from scale_configs (sample scale).
     */
    private Map<String, String> dataFieldNames;

    /**
     * Effective per-field aggregation method after applying defaults and special rules.
     */
    private Map<String, String> aggregationByField;

    /**
     * Ratio formula used (e.g., "data_1/data_3").
     */
    private String ratioFormula;

    /**
     * Overview statistics for all data fields, broken down by direction.
     * Key: direction ("0" = unknown, "1" = import/nhập, "2" = export/xuất)
     * Value: Map of field statistics
     * - data_1: SUM
     * - data_2..data_5: AVG
     */
    private Map<String, Map<String, OverviewStats>> overview;

    private List<Row> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private ScaleInfo scale;
        private String period;
        private Integer recordCount;

        /**
         * Direction code: 0 = unknown, 1 = import/nhập, 2 = export/xuất
         */
        private Integer direction;

        /**
         * Data field values at the start of the period.
         * Example: "data_1": {"value":"100.5","name":"Weight","used":true}
         */
        @JsonProperty("start_values")
        private Map<String, DataFieldValue> startValues;

        /**
         * Data field values at the end of the period.
         * Example: "data_1": {"value":"150.3","name":"Weight","used":true}
         */
        @JsonProperty("end_values")
        private Map<String, DataFieldValue> endValues;

        /**
         * Aggregated data field values for the period (same as V1).
         * Example: "data_1": {"value":"250.8","name":"Weight","used":true}
         */
        @JsonProperty("data_values")
        private Map<String, DataFieldValue> dataValues;

        /**
         * Calculated ratio based on ratioFormula (e.g., data_1/data_3).
         * Example: {"value":"1.25","formula":"data_1/data_3"}
         */
        private RatioValue ratio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataFieldValue {
        private String value;
        private String name;

        @JsonProperty("used")
        private boolean used;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RatioValue {
        private String value;
        private String formula;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OverviewStats {
        private String value;
        private String aggregation; // SUM or AVG
        private String name;

        @JsonProperty("used")
        private boolean used;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScaleInfo {
        private Long id;
        private String name;
        private String model;
        private String type;
        private String direction;

        @JsonProperty("is_active")
        private Boolean isActive;

        private LocationInfo location;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("created_by")
        private String createdBy;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @JsonProperty("updated_by")
        private String updatedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocationInfo {
        private Long id;
        private String code;
        private String name;
        private String description;

        @JsonProperty("parent_id")
        private Long parentId;
    }
}
