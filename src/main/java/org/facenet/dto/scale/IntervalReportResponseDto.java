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
 * Response DTO for interval statistics report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntervalReportResponseDto {

    private IntervalReportRequestDto.TimeInterval interval;
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
         * Per-field values keyed by data_1..data_5.
         * Example:
         * "data_2": {"value":"0","name":"Status","used":true}
         */
        @JsonProperty("data_values")
        private Map<String, DataFieldValue> dataValues;
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
    public static class ScaleInfo {
        private Long id;
        private String name;
        private String model;
        private String type;

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
