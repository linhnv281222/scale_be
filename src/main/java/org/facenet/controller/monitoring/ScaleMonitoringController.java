package org.facenet.controller.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
import org.facenet.entity.monitoring.ScaleHealthStatus;
import org.facenet.service.monitoring.ScaleMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for scale monitoring and health check APIs
 */
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
@Slf4j
public class ScaleMonitoringController {

    private final ScaleMonitoringService monitoringService;

    /**
     * Get monitoring statistics
     * GET /api/v1/monitoring/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ScaleMonitoringService.MonitoringStatistics>> getStatistics() {
        ScaleMonitoringService.MonitoringStatistics stats = monitoringService.getMonitoringStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get all active issues
     * GET /api/v1/monitoring/issues
     */
    @GetMapping("/issues")
    public ResponseEntity<ApiResponse<List<ScaleHealthStatus>>> getAllActiveIssues() {
        List<ScaleHealthStatus> issues = monitoringService.getAllActiveIssues();
        return ResponseEntity.ok(ApiResponse.success(issues));
    }

    /**
     * Get active issues for a specific scale
     * GET /api/v1/monitoring/issues/{scaleId}
     */
    @GetMapping("/issues/{scaleId}")
    public ResponseEntity<ApiResponse<List<ScaleHealthStatus>>> getScaleIssues(@PathVariable Long scaleId) {
        List<ScaleHealthStatus> issues = monitoringService.getActiveIssues(scaleId);
        return ResponseEntity.ok(ApiResponse.success(issues));
    }

    /**
     * Check health of a specific scale on-demand
     * POST /api/v1/monitoring/check/{scaleId}
     */
    @PostMapping("/check/{scaleId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MONITOR_SCALES')")
    public ResponseEntity<ApiResponse<ScaleMonitoringService.ScaleHealthResult>> checkScaleHealth(
            @PathVariable Long scaleId) {
        ScaleMonitoringService.ScaleHealthResult result = monitoringService.checkScaleHealth(scaleId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Trigger manual health check for all scales
     * POST /api/v1/monitoring/check-all
     */
    @PostMapping("/check-all")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MONITOR_SCALES')")
    public ResponseEntity<ApiResponse<String>> checkAllScales() {
        try {
            monitoringService.monitorScalesHealth();
            return ResponseEntity.ok(ApiResponse.success("Health check completed successfully"));
        } catch (Exception e) {
            log.error("Error during manual health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Health check failed: " + e.getMessage()));
        }
    }
}
