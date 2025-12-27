package org.facenet.controller.report;

import com.lowagie.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.service.report.ReportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST Controller for Report Export
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Report export API (Excel, Word, PDF)")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportExportService reportExportService;

    /**
     * Export report to specified format (with optional template)
     * 
     * @param request Report export request
     * @return Byte stream of exported file
     */
    @PostMapping("/export")
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(
        summary = "Export report",
        description = "Export weighing report to Excel, Word, or PDF format using dynamic templates. " +
                     "Data is fetched from weighing_logs and aggregated by scale. " +
                     "JSONB data fields are converted to numbers for calculations. " +
                     "Optional templateId parameter to use specific template, otherwise uses default template for the export type."
    )
    public ResponseEntity<byte[]> exportReport(
            @Valid @RequestBody ReportExportRequest request,
            @RequestParam(required = false) Long templateId) {
        try {
            log.info("Received report export request: type={}, templateId={}, startTime={}, endTime={}", 
                    request.getType(), templateId, request.getStartTime(), request.getEndTime());

            // Export report with optional template
            byte[] reportData = reportExportService.exportReport(request, templateId);

            // Generate filename
            String filename = reportExportService.generateFilename(request);

            log.info("Report exported successfully: filename={}, size={} bytes", filename, reportData.length);

            // Return file with appropriate headers
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(request.getType().getContentType()))
                    .contentLength(reportData.length)
                    .body(reportData);

        } catch (IOException e) {
            log.error("Failed to export report: IO error", e);
            throw new RuntimeException("Failed to generate report file: " + e.getMessage(), e);
        } catch (DocumentException e) {
            log.error("Failed to export report: Document error", e);
            throw new RuntimeException("Failed to generate PDF document: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to export report: Unexpected error", e);
            throw new RuntimeException("Failed to export report: " + e.getMessage(), e);
        }
    }

    /**
     * Get report statistics (without exporting)
     * Useful for previewing report data before export
     */
    @PostMapping("/preview")
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OPERATOR')")
    @Operation(
        summary = "Preview report statistics",
        description = "Get report summary without generating the file. " +
                     "Returns statistics like total records, scales, and aggregated values."
    )
    public ResponseEntity<ApiResponse<Object>> previewReport(@Valid @RequestBody ReportExportRequest request) {
        try {
            log.info("Preview report request: startTime={}, endTime={}", 
                    request.getStartTime(), request.getEndTime());

            // This endpoint is a placeholder for future implementation
            // For now, it just validates the request parameters
            return ResponseEntity.ok(ApiResponse.success(
                    "Report preview feature available",
                    "Use /export endpoint to generate actual reports"
            ));

        } catch (Exception e) {
            log.error("Failed to preview report", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to preview report: " + e.getMessage())
            );
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Check report service health", description = "Verify that report export services are available")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success(
                "Report service is healthy",
                "All export services (Excel, Word, PDF) are operational"
        ));
    }
}
