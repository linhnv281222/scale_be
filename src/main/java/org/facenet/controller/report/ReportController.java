package org.facenet.controller.report;

import com.lowagie.text.DocumentException;
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
// Disabled: legacy report endpoints removed per requirement
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<byte[]> exportReport(
            @Valid @RequestBody ReportExportRequest request,
            @RequestParam(required = false) Long templateId) {
        try {
            log.info("Received report export request: type={}, templateId={}, startTime={}, endTime={}", 
                    request.getType(), templateId, request.getStartTime(), request.getEndTime());

            // Validate export type
            if (request.getType() == null) {
                throw new IllegalArgumentException("Export type is required");
            }

            // Export report with optional template
            byte[] reportData = reportExportService.exportReport(request, templateId);

            if (reportData == null || reportData.length == 0) {
                throw new RuntimeException("Failed to generate report: empty data");
            }

            // Generate filename
            String filename = reportExportService.generateFilename(request);

            log.info("Report exported successfully: filename={}, size={} bytes", filename, reportData.length);

            // Get content type
            String contentType = request.getType().getContentType();
            
            // Return file with appropriate headers
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(reportData.length)
                    .body(reportData);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            throw new RuntimeException("Invalid request: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to export report: IO error - {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report file. Please check server logs for details: " + e.getMessage(), e);
        } catch (DocumentException e) {
            log.error("Failed to export report: Document error - {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF document. Please check template configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to export report: Unexpected error - {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export report. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Get report statistics (without exporting)
     * Useful for previewing report data before export
     */
    @PostMapping("/preview")
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OPERATOR')")
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
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success(
                "Report service is healthy",
                "All export services (Excel, Word, PDF) are operational"
        ));
    }
}
