package org.facenet.controller.report;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.report.IntervalReportExportRequestV2;
import org.facenet.dto.report.ReportExportRequest;
import org.facenet.entity.report.ReportDefinition;
import org.facenet.entity.report.ReportExecutionHistory;
import org.facenet.service.report.ReportDefinitionService;
import org.facenet.service.report.ReportExecutionHistoryService;
import org.facenet.service.report.ReportExportService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Report Controller
 * Handles report export through Report Definition system
 * All exports must go through defined report codes
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class EnterpriseReportController {

    private final ReportDefinitionService reportDefinitionService;
    private final ReportExecutionHistoryService executionHistoryService;
    private final ReportExportService reportExportService;

    /**
     * Export report using imported template (SIMPLIFIED APPROACH)
     * Uses template from template_imports table directly
     * No need for report code or report definition
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportWithTemplate(
            @RequestParam(name = "importId", required = true) Long importId,
            
            @Valid @RequestBody ReportExportRequest request,
            
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Report export with template import: importId={}, user={}", 
                    importId, getCurrentUser());

            // 1. Validate importId
            if (importId == null) {
                throw new IllegalArgumentException("importId must not be null");
            }
            
            // 2. Export report with template
            byte[] reportData = reportExportService.exportReportWithImportedTemplate(request, importId);
            
            // 3. Generate filename
            String filename = reportExportService.generateFilename(request);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Report with template import {} exported successfully: {} bytes, {}ms", 
                    importId, reportData.length, executionTime);
            
            // 4. Return file
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header("X-Import-Id", importId.toString())
                    .header("X-Execution-Time-Ms", String.valueOf(executionTime))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .contentLength(reportData.length)
                    .body(reportData);

        } catch (IllegalArgumentException e) {
            log.error("Report validation failed: {}", e.getMessage());
            throw new RuntimeException("Report validation failed: " + e.getMessage(), e);
            
        } catch (IOException | DocumentException e) {
            log.error("Report export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("Unexpected error during report export", e);
            throw new RuntimeException("Report export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Export Interval Report V2 with template
     * Combines interval/v2 data generation with template-based export
     * Supports direction-based overview and enhanced statistics
     */
    @PostMapping("/export/v2")
    public ResponseEntity<byte[]> exportIntervalReportV2(
            @Valid @RequestBody IntervalReportExportRequestV2 request,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Interval Report V2 export: importId={}, interval={}, fromTime={}, toTime={}, user={}", 
                    request.getImportId(), request.getInterval(), request.getFromTime(), 
                    request.getToTime(), getCurrentUser());

            // Validate importId
            if (request.getImportId() == null) {
                throw new IllegalArgumentException("importId must not be null");
            }
            
            // Export interval report V2 with template
            byte[] reportData = reportExportService.exportIntervalReportV2WithTemplate(request);
            
            // Generate filename
            String filename = reportExportService.generateFilenameForIntervalV2(request);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Interval Report V2 exported successfully: {} bytes, {}ms", 
                    reportData.length, executionTime);
            
            // Determine content type from filename
            MediaType contentType = determineContentType(filename);
            
            // Return file
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header("X-Import-Id", request.getImportId().toString())
                    .header("X-Execution-Time-Ms", String.valueOf(executionTime))
                    .contentType(contentType)
                    .contentLength(reportData.length)
                    .body(reportData);

        } catch (IllegalArgumentException e) {
            log.error("Interval Report V2 validation failed: {}", e.getMessage());
            throw new RuntimeException("Report validation failed: " + e.getMessage(), e);
            
        } catch (IOException | DocumentException e) {
            log.error("Interval Report V2 export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("Unexpected error during Interval Report V2 export", e);
            throw new RuntimeException("Report export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get list of available reports
     */
    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<List<ReportDefinition>>> getReportDefinitions() {
        List<ReportDefinition> reports = reportDefinitionService.getExecutableReports();
        return ResponseEntity.ok(ApiResponse.success(
                reports,
                "Retrieved " + reports.size() + " report definitions"
        ));
    }

    /**
     * Get report definition by code
     */
    @GetMapping("/definitions/{reportCode}")
    public ResponseEntity<ApiResponse<ReportDefinition>> getReportDefinition(
            @PathVariable(name = "reportCode") String reportCode) {
        ReportDefinition definition = reportDefinitionService.getByCode(reportCode);
        return ResponseEntity.ok(ApiResponse.success(
                definition,
                "Report definition found"
        ));
    }

    /**
     * Get execution history for a report
     */
    @GetMapping("/{reportCode}/history")
    public ResponseEntity<ApiResponse<Page<ReportExecutionHistory>>> getExecutionHistory(
            @PathVariable(name = "reportCode") String reportCode,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        Page<ReportExecutionHistory> history = executionHistoryService.getHistoryByReportCode(
                reportCode,
                org.springframework.data.domain.PageRequest.of(page, size)
        );
        
        return ResponseEntity.ok(ApiResponse.success(
                history,
                String.format("Found %d executions for report %s", history.getTotalElements(), reportCode)
        ));
    }

    /**
     * Get recent executions across all reports
     */
    @GetMapping("/history/recent")
    public ResponseEntity<ApiResponse<Page<ReportExecutionHistory>>> getRecentExecutions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        Page<ReportExecutionHistory> history = executionHistoryService.getRecentExecutions(page, size);
        
        return ResponseEntity.ok(ApiResponse.success(
                history,
                "Retrieved recent executions"
        ));
    }

    /**
     * Get execution statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Long> stats = executionHistoryService.getStatistics();
        
        Map<String, Object> response = new HashMap<>(stats);
        response.put("executionDate", OffsetDateTime.now());
        
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Execution statistics"
        ));
    }

    // Helper methods

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
    
    /**
     * Determine content type from filename extension
     */
    private MediaType determineContentType(String filename) {
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if (lowerFilename.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (lowerFilename.endsWith(".xlsx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (lowerFilename.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
