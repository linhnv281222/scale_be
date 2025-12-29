package org.facenet.controller.report;

import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
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
import java.util.Locale;
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
     * Export report by report code (ENTERPRISE STANDARD)
     * This is the primary method for exporting reports
     */
    @PostMapping("/{reportCode}/export")
    public ResponseEntity<byte[]> exportByCode(
            @PathVariable(name = "reportCode") String reportCode,
            
            @RequestParam(name = "format") String format,
            
            @Valid @RequestBody ReportExportRequest request,
            
            @RequestParam(name = "templateId", required = false) Long templateId,
            
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        Long executionId = null;
        
        try {
            log.info("Report export request: code={}, format={}, user={}", 
                    reportCode, format, getCurrentUser());

            // 1. Validate Report Code
            ReportDefinition reportDefinition = reportDefinitionService.getByCode(reportCode);
            
            // 2. Check Report Status
            reportDefinitionService.validateExecutable(reportCode);
            
            // 3. Validate Format
            if (format == null || format.trim().isEmpty()) {
                throw new IllegalArgumentException("format must not be blank");
            }
            reportDefinitionService.validateFormat(reportCode, format.trim().toUpperCase(Locale.ROOT));
            
            // 4. Start execution tracking
            Map<String, Object> parameters = buildParameters(request, format, templateId);
            executionId = startExecutionTracking(reportDefinition, format, parameters, httpRequest);
            
            // 5. Execute Report
            byte[] reportData = reportExportService.exportReport(request, templateId);
            
            // 6. Generate filename
            String filename = reportExportService.generateFilename(request);
            
            // 7. Mark execution successful
            executionHistoryService.markSuccess(
                    executionId, 
                    getRecordCount(reportData), 
                    filename, 
                    reportData.length
            );
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Report {} exported successfully: {} bytes, {}ms", 
                    reportCode, reportData.length, executionTime);
            
            // 8. Return file
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header("X-Report-Code", reportCode)
                    .header("X-Execution-Id", executionId.toString())
                    .header("X-Execution-Time-Ms", String.valueOf(executionTime))
                    .contentType(getMediaType(format))
                    .contentLength(reportData.length)
                    .body(reportData);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Validation errors
            if (executionId != null) {
                executionHistoryService.markFailed(executionId, e);
            }
            log.error("Report validation failed: {}", e.getMessage());
            throw new RuntimeException("Report validation failed: " + e.getMessage(), e);
            
        } catch (IOException | DocumentException e) {
            // Export errors
            if (executionId != null) {
                executionHistoryService.markFailed(executionId, new Exception(e));
            }
            log.error("Report export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
            
        } catch (Exception e) {
            // Unexpected errors
            if (executionId != null) {
                executionHistoryService.markFailed(executionId, e);
            }
            log.error("Unexpected error during report export", e);
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

    private Long startExecutionTracking(ReportDefinition definition, String format, 
                                        Map<String, Object> parameters, HttpServletRequest request) {
        String user = getCurrentUser();
        ReportExecutionHistory history = executionHistoryService.startExecution(
                definition, format.toUpperCase(), parameters, user);
        
        // Update client info
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        executionHistoryService.updateClientInfo(history.getId(), clientIp, userAgent);
        
        return history.getId();
    }

    private Map<String, Object> buildParameters(ReportExportRequest request, String format, Long templateId) {
        Map<String, Object> params = new HashMap<>();
        params.put("format", format);
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("scaleIds", request.getScaleIds());
        params.put("reportTitle", request.getReportTitle());
        params.put("reportCode", request.getReportCode());
        params.put("preparedBy", request.getPreparedBy());
        params.put("templateId", templateId);
        return params;
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private MediaType getMediaType(String format) {
        return switch (format.toUpperCase()) {
            case "EXCEL" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "WORD" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "PDF" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private int getRecordCount(byte[] data) {
        // Placeholder - actual implementation would parse the data
        // For now, return -1 to indicate unknown
        return -1;
    }
}
