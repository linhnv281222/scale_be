package org.facenet.controller.monitoring;

import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.event.AllScalesDataEvent;
import org.facenet.event.ScaleSummaryEvent;
import org.facenet.service.monitoring.ScaleMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for monitoring scale status
 */
// Disabled: monitoring endpoints removed per requirement
@RequiredArgsConstructor
public class MonitoringController {

    private final ScaleMonitoringService monitoringService;

    /**
     * Get scale summary
     */
    @GetMapping("/scale-summary")
    public ResponseEntity<ApiResponse<ScaleSummaryEvent>> getScaleSummary() {
        ScaleSummaryEvent summary = monitoringService.getScaleSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Broadcast scale summary immediately
     */
    @PostMapping("/scale-summary/broadcast")
    public ResponseEntity<ApiResponse<ScaleSummaryEvent>> broadcastScaleSummary() {
        ScaleSummaryEvent summary = monitoringService.broadcastScaleSummaryNow();
        return ResponseEntity.ok(ApiResponse.success(summary, "Scale summary broadcasted"));
    }

    /**
     * Get all scales data (current state of all scales)
     */
    @GetMapping("/all-scales-data")
    public ResponseEntity<ApiResponse<AllScalesDataEvent>> getAllScalesData() {
        AllScalesDataEvent allData = monitoringService.getAllScalesData();
        return ResponseEntity.ok(ApiResponse.success(allData));
    }
}
