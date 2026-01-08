package org.facenet.controller.scale;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.scale.IntervalReportRequestDto;
import org.facenet.dto.scale.IntervalReportResponseDto;
import org.facenet.dto.scale.ReportRequestDto;
import org.facenet.dto.scale.ReportResponseDto;
import org.facenet.service.scale.report.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for scale report and statistics endpoints
 */
@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ScaleReportController {

    private final ReportService reportService;

    /**
     * Generate report based on request parameters
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ReportResponseDto>> generateReport(
            @Valid @RequestBody ReportRequestDto request) {

        log.info("[REPORT-API] Generating report: {}", request);

        try {
            ReportResponseDto report = reportService.generateReport(request);
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            log.error("[REPORT-API] Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate report: " + e.getMessage()));
        }
    }

    /**
     * Generate interval statistics report (SHIFT/HOUR/DAY/WEEK) with per-field aggregation.
     * Supports filtering by manufacturer, location, and scale direction.
     */
    @PostMapping("/interval")
    public ResponseEntity<ApiResponse<IntervalReportResponseDto>> generateIntervalReport(
            @Valid @RequestBody IntervalReportRequestDto request) {

        log.info("[REPORT-API] Generating interval report: {}", request);

        try {
            IntervalReportResponseDto report = reportService.generateIntervalReport(request);
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            log.error("[REPORT-API] Error generating interval report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate interval report: " + e.getMessage()));
        }
    }

    /**
     * Manually trigger daily aggregation (for testing/admin)
     */
    @PostMapping("/aggregate-daily")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<String>> aggregateDailyData() {

        log.info("[REPORT-API] Manually triggering daily aggregation");

        try {
            reportService.aggregateDailyData();
            return ResponseEntity.ok(ApiResponse.success("Daily aggregation completed successfully"));
        } catch (Exception e) {
            log.error("[REPORT-API] Error during manual aggregation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to aggregate data: " + e.getMessage()));
        }
    }
}
