package org.facenet.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class ReportDesignerDto {
    private ReportDesignerDto() {
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TemplateAnalysis {
        @JsonProperty("import_id") private Long importId;
        @JsonProperty("template_id") private Long templateId;
        private String filename;
        @JsonProperty("file_type") private String fileType;
        private List<String> placeholders;
        private List<SheetAnalysis> sheets;
        private TemplateMapping mapping;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SheetAnalysis {
        private String name;
        @JsonProperty("last_row") private int lastRow;
        @JsonProperty("last_column") private int lastColumn;
        private List<PlaceholderPosition> placeholders;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlaceholderPosition {
        private String placeholder;
        private Integer row;
        private Integer column;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TemplateMappingRequest {
        @JsonProperty("placeholder_mappings") private Map<String, String> placeholderMappings;
        @JsonProperty("sheet_name") private String sheetName;
        @JsonProperty("header_row") @Min(0) private Integer headerRow;
        @JsonProperty("data_start_row") @Min(0) private Integer dataStartRow;
        @JsonProperty("row_offset") @Min(0) private Integer rowOffset;
        @JsonProperty("column_offset") @Min(0) private Integer columnOffset;
        @JsonProperty("column_mappings") private Map<String, String> columnMappings;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TemplateMapping {
        @JsonProperty("placeholder_mappings") private Map<String, String> placeholderMappings;
        @JsonProperty("sheet_name") private String sheetName;
        @JsonProperty("header_row") private Integer headerRow;
        @JsonProperty("data_start_row") private Integer dataStartRow;
        @JsonProperty("row_offset") private Integer rowOffset;
        @JsonProperty("column_offset") private Integer columnOffset;
        @JsonProperty("column_mappings") private Map<String, String> columnMappings;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetailQueryRequest {
        @JsonProperty("scale_ids") private List<Long> scaleIds;
        @JsonProperty("location_ids") private List<Long> locationIds;
        @NotNull private OffsetDateTime from;
        @NotNull private OffsetDateTime to;
        private List<String> fields;
        @JsonProperty("row_step") @Min(1) @Max(10000) private Integer rowStep;
        @Min(0) private Integer page;
        @Min(1) @Max(5000) private Integer size;
        @JsonProperty("sort_direction") private String sortDirection;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DetailQueryResponse {
        private OffsetDateTime from;
        private OffsetDateTime to;
        @JsonProperty("row_step") private int rowStep;
        private int page;
        private int size;
        @JsonProperty("total_rows") private long totalRows;
        @JsonProperty("total_pages") private long totalPages;
        private List<String> fields;
        private List<Map<String, Object>> rows;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChartRequest {
        @JsonProperty("scale_ids") private List<Long> scaleIds;
        @JsonProperty("location_ids") private List<Long> locationIds;
        @NotNull private OffsetDateTime from;
        @NotNull private OffsetDateTime to;
        private List<String> fields;
        private String bucket;
        private String aggregation;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChartResponse {
        private OffsetDateTime from;
        private OffsetDateTime to;
        private String bucket;
        private String aggregation;
        private List<ChartSeries> series;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChartSeries {
        @JsonProperty("scale_id") private Long scaleId;
        @JsonProperty("scale_name") private String scaleName;
        private String field;
        private List<ChartPoint> points;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChartPoint {
        private OffsetDateTime timestamp;
        private BigDecimal value;
    }
}
