package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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
}
