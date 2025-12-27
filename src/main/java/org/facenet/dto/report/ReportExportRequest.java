package org.facenet.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for report export request
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

    private String method; // "weighing_logs" or "daily_reports"

    private String reportTitle;

    private String reportCode;

    private String preparedBy;
}
