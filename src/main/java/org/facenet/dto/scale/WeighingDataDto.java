package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTOs for WeighingLog and Report operations
 */
public class WeighingDataDto {

    /**
     * Response DTO for weighing log entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LogResponse {
        private Long scaleId;
        private String scaleName;
        private OffsetDateTime createdAt;
        private OffsetDateTime lastTime;
        private String data1;
        private String data2;
        private String data3;
        private String data4;
        private String data5;

        /**
         * Map data field to {value, name, isUsed} derived from scale_config.
         * Keys are: data_1 .. data_5
         */
        private Map<String, DataFieldValue> dataValues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataFieldValue {
        private String value;
        private String name;
        private boolean isUsed;
    }

    /**
     * Response DTO for daily report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DailyReportResponse {
        private LocalDate date;
        private Long scaleId;
        private String scaleName;
        private OffsetDateTime lastTime;
        private String data1;
        private String data2;
        private String data3;
        private String data4;
        private String data5;
    }

    /**
     * Request DTO for querying weighing logs
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogQueryRequest {
        private Long scaleId;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private Integer page;
        private Integer size;
    }

    /**
     * Response DTO for weighing history with scale details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HistoryResponse {
        private Long scaleId;
        private String scaleCode;
        private String scaleName;
        private String scaleDirection;
        private String locationName;
        private String protocolName;
        private OffsetDateTime createdAt;
        private OffsetDateTime lastTime;
        private String data1;
        private String data2;
        private String data3;
        private String data4;
        private String data5;
        
        /**
         * Map data field to {value, name, isUsed} derived from scale_config.
         * Keys are: data_1 .. data_5
         */
        private Map<String, DataFieldValue> dataValues;
    }
}
