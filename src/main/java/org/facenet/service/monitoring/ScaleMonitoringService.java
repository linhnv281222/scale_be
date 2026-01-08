package org.facenet.service.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.monitoring.ScaleHealthStatus;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.event.AllScalesDataEvent;
import org.facenet.event.DataField;
import org.facenet.event.ScaleAlertEvent;
import org.facenet.event.ScaleBatchAlertEvent;
import org.facenet.event.ScaleSummaryEvent;
import org.facenet.repository.monitoring.ScaleHealthStatusRepository;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.service.scale.engine.EngineManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service để broadcast tổng quan số lượng cân và giám sát sức khỏe
 * Chạy định kỳ để cập nhật frontend về trạng thái tổng thể
 * Phát hiện và cảnh báo các vấn đề kết nối, đọc dữ liệu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScaleMonitoringService {

    private final ScaleRepository scaleRepository;
    private final ScaleCurrentStateRepository currentStateRepository;
    private final ScaleHealthStatusRepository healthStatusRepository;
    private final EngineManager engineManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // Configuration thresholds
    private static final int NO_DATA_THRESHOLD_MINUTES = 5;
    private static final int CONSECUTIVE_FAILURES_ALERT_THRESHOLD = 1; // Changed from 3 to 1 for immediate alerts
    private static final int STALE_DATA_THRESHOLD_MINUTES = 3;


    /**
     * Broadcast scale summary mỗi 5 giây
     */
    @Scheduled(fixedRate = 50000)
    public void broadcastScaleSummary() {
        try {
            ScaleSummaryEvent summary = buildScaleSummary();
            
            // Broadcast qua WebSocket
            messagingTemplate.convertAndSend("/topic/scale-summary", summary);
            
            log.debug("[MONITORING] Broadcasted scale summary: total={}, active={}, online={}", 
                    summary.getTotalScales(), summary.getActiveScales(), summary.getOnlineScales());
        } catch (Exception e) {
            log.error("[MONITORING] Error broadcasting scale summary: {}", e.getMessage());
        }
    }

    /**
     * Broadcast toàn bộ dữ liệu của tất cả các cân mỗi 2 giây
     */
    @Scheduled(fixedRate = 180000)
    public void broadcastAllScalesData() {
        try {
            AllScalesDataEvent allData = buildAllScalesData();
            
            // Broadcast qua WebSocket
            messagingTemplate.convertAndSend("/topic/all-scales-data", allData);
            
            log.debug("[MONITORING] Broadcasted all scales data: {} scales", allData.getScales().size());
        } catch (Exception e) {
            log.error("[MONITORING] Error broadcasting all scales data: {}", e.getMessage());
        }
    }

    /**
     * Broadcast scale summary on-demand
     */
    public ScaleSummaryEvent broadcastScaleSummaryNow() {
        ScaleSummaryEvent summary = buildScaleSummary();
        messagingTemplate.convertAndSend("/topic/scale-summary", summary);
        log.info("[MONITORING] Broadcasted scale summary on-demand");
        return summary;
    }

    /**
     * Build scale summary từ database và engine manager
     */
    private ScaleSummaryEvent buildScaleSummary() {
        // Tổng số cân trong DB
        long totalScales = scaleRepository.count();
        
        // Số cân active
        long activeScales = scaleRepository.countByIsActive(true);
        
        // Số cân đang online (có engine đang chạy)
        int onlineScales = 0;
        for (Long scaleId : engineManager.getRunningEngines().keySet()) {
            if (engineManager.isEngineRunning(scaleId)) {
                onlineScales++;
            }
        }
        
        // Số cân offline = active nhưng không có engine
        int offlineScales = (int) (activeScales - onlineScales);
        
        return ScaleSummaryEvent.builder()
                .totalScales((int) totalScales)
                .activeScales((int) activeScales)
                .onlineScales(onlineScales)
                .offlineScales(offlineScales)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Get scale summary without broadcasting
     */
    public ScaleSummaryEvent getScaleSummary() {
        return buildScaleSummary();
    }

    /**
     * Get all scales data without broadcasting
     */
    public AllScalesDataEvent getAllScalesData() {
        return buildAllScalesData();
    }

    /**
     * Build all scales data từ current states
     */
    private AllScalesDataEvent buildAllScalesData() {
        List<AllScalesDataEvent.ScaleData> scaleDataList = new ArrayList<>();
        
        // Lấy tất cả scales active
        List<Scale> activeScales = scaleRepository.findByIsActive(true);
        
        for (Scale scale : activeScales) {
            // Lấy current state
            ScaleCurrentState currentState = currentStateRepository.findById(scale.getId())
                    .orElse(null);
            
            if (currentState != null) {
                AllScalesDataEvent.ScaleData scaleData = AllScalesDataEvent.ScaleData.builder()
                        .scaleId(scale.getId())
                        .scaleName(scale.getName())
                        .status(currentState.getStatus())
                        .lastTime(currentState.getLastTime() != null ? 
                                ZonedDateTime.from(currentState.getLastTime()) : null)
                        .data1(parseDataField(currentState.getData1()))
                        .data2(parseDataField(currentState.getData2()))
                        .data3(parseDataField(currentState.getData3()))
                        .data4(parseDataField(currentState.getData4()))
                        .data5(parseDataField(currentState.getData5()))
                        .build();
                
                scaleDataList.add(scaleData);
            }
        }
        
        return AllScalesDataEvent.builder()
                .scales(scaleDataList)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Parse data field từ String trong DB
     * Tạm thời chỉ có value, không có name (vì DB chỉ lưu value)
     */
    private DataField parseDataField(String value) {
        if (value == null) {
            return null;
        }
        return DataField.builder()
                .value(value)
                .build();
    }

    // ==================== HEALTH MONITORING METHODS ====================

    /**
     * Check health of all active scales (scheduled every 2 minutes)
     */
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    @Transactional
    public void monitorScalesHealth() {
        try {
            log.info("[HEALTH-CHECK] Starting health monitoring for all scales");
            
            List<Scale> activeScales = scaleRepository.findByIsActive(true);
            log.info("[HEALTH-CHECK] Found {} active scale(s)", activeScales.size());
            OffsetDateTime now = OffsetDateTime.now();
            
            List<ScaleAlertEvent> pendingAlerts = new ArrayList<>();
            List<ScaleAlertEvent> recoveryAlerts = new ArrayList<>();
            int issuesDetected = 0;
            
            for (Scale scale : activeScales) {
                try {
                    ScaleHealthCheckResult checkResult = checkAndHandleScaleHealth(scale, now);
                    
                    if (checkResult.hasIssue) {
                        issuesDetected++;
                        if (checkResult.alertTriggered && checkResult.alert != null) {
                            pendingAlerts.add(checkResult.alert);
                        }
                    } else if (checkResult.hasRecovery && checkResult.alert != null) {
                        recoveryAlerts.add(checkResult.alert);
                    }
                } catch (Exception e) {
                    log.error("[HEALTH-CHECK] Error checking scale {}: {}", scale.getId(), e.getMessage());
                }
            }
            
            // Process alerts based on count
            processAlerts(pendingAlerts, recoveryAlerts, now);
            
            log.info("[HEALTH-CHECK] Completed: {} scales checked, {} issues detected, {} alerts, {} recoveries", 
                activeScales.size(), issuesDetected, pendingAlerts.size(), recoveryAlerts.size());
                
        } catch (Exception e) {
            log.error("[HEALTH-CHECK] Error in health monitoring: {}", e.getMessage(), e);
        }
    }

    /**
     * Process alerts - always send as batch (array format) regardless of count
     */
    private void processAlerts(List<ScaleAlertEvent> issueAlerts, List<ScaleAlertEvent> recoveryAlerts, OffsetDateTime now) {
        log.info("[ALERT BATCH] Processing: {} issue alerts, {} recovery alerts", issueAlerts.size(), recoveryAlerts.size());
        
        // Handle issue alerts - always send as batch
        if (!issueAlerts.isEmpty()) {
            log.warn("[ALERT BATCH] Sending issue batch for {} scale(s)", issueAlerts.size());
            sendBatchAlert(issueAlerts, now, false);
            log.warn("[ALERT BATCH] Issue batch sent successfully");
        } else {
            log.debug("[ALERT BATCH] No issue alerts to send");
        }
        
        // Handle recovery alerts - also send as batch
        if (!recoveryAlerts.isEmpty()) {
            log.info("[ALERT BATCH] Sending recovery batch for {} scale(s)", recoveryAlerts.size());
            sendBatchAlert(recoveryAlerts, now, true);
            log.info("[ALERT BATCH] Recovery batch sent successfully");
        } else {
            log.debug("[ALERT BATCH] No recovery alerts to send");
        }
    }

    /**
     * Send batch alert event
     */
    private void sendBatchAlert(List<ScaleAlertEvent> alerts, OffsetDateTime now, boolean isRecovery) {
        ScaleBatchAlertEvent.AlertSummary summary = buildAlertSummary(alerts);
        
        ScaleBatchAlertEvent batchEvent = ScaleBatchAlertEvent.builder()
            .timestamp(now)
            .totalIssues(alerts.size())
            .alerts(alerts)
            .summary(summary)
            .isRecoveryBatch(isRecovery)
            .build();
        
        // Publish batch event
        eventPublisher.publishEvent(batchEvent);
        
        // Broadcast via WebSocket - single channel for all alerts
        messagingTemplate.convertAndSend("/topic/scale-alerts", batchEvent);
    }

    /**
     * Build alert summary statistics
     */
    private ScaleBatchAlertEvent.AlertSummary buildAlertSummary(List<ScaleAlertEvent> alerts) {
        ScaleBatchAlertEvent.AlertSummary summary = new ScaleBatchAlertEvent.AlertSummary();
        
        for (ScaleAlertEvent alert : alerts) {
            // Count by alert type
            switch (alert.getAlertType()) {
                case CRITICAL -> summary.setCriticalCount(summary.getCriticalCount() + 1);
                case WARNING -> summary.setWarningCount(summary.getWarningCount() + 1);
                case INFO -> summary.setInfoCount(summary.getInfoCount() + 1);
                case RECOVERY -> summary.setRecoveryCount(summary.getRecoveryCount() + 1);
            }
            
            // Count by issue type
            if (alert.getIssueType() != null) {
                switch (alert.getIssueType()) {
                    case ZERO_VALUE -> summary.setZeroValueCount(summary.getZeroValueCount() + 1);
                    case NO_SIGNAL -> summary.setNoSignalCount(summary.getNoSignalCount() + 1);
                    case CONNECTION_LOST, NETWORK_ERROR -> 
                        summary.setConnectionLostCount(summary.getConnectionLostCount() + 1);
                    case READ_ERROR, PROTOCOL_ERROR -> 
                        summary.setReadErrorCount(summary.getReadErrorCount() + 1);
                    case DATA_STALE -> summary.setStaleDataCount(summary.getStaleDataCount() + 1);
                    default -> summary.setOtherCount(summary.getOtherCount() + 1);
                }
            }
        }
        
        return summary;
    }

    /**
     * Check health of a specific scale
     */
    @Transactional
    public ScaleHealthResult checkScaleHealth(Long scaleId) {
        Scale scale = scaleRepository.findById(scaleId)
            .orElseThrow(() -> new RuntimeException("Scale not found: " + scaleId));
        
        return performHealthCheck(scale, OffsetDateTime.now());
    }

    /**
     * Internal method to check and handle scale health
     * Returns result with alert information
     */
    private ScaleHealthCheckResult checkAndHandleScaleHealth(Scale scale, OffsetDateTime now) {
        ScaleHealthCheckResult checkResult = new ScaleHealthCheckResult();
        ScaleHealthResult result = performHealthCheck(scale, now);
        
        checkResult.hasIssue = result.hasIssue;
        
        if (result.hasIssue) {
            ScaleAlertEvent alert = handleScaleIssue(scale, result, now);
            if (alert != null) {
                checkResult.alertTriggered = true;
                checkResult.alert = alert;
            }
        } else {
            List<ScaleAlertEvent> recoveryAlerts = resolveExistingIssues(scale, now);
            if (!recoveryAlerts.isEmpty()) {
                checkResult.hasRecovery = true;
                checkResult.alert = recoveryAlerts.get(0); // Take first for now
            }
        }
        
        return checkResult;
    }

    /**
     * Perform health check on a scale
     */
    private ScaleHealthResult performHealthCheck(Scale scale, OffsetDateTime now) {
        log.debug("[HEALTH-CHECK] Checking scale ID={}, Name={}", scale.getId(), scale.getName());
        
        ScaleHealthResult result = new ScaleHealthResult();
        result.scaleId = scale.getId();
        result.scaleName = scale.getName();
        result.checkTime = now;
        
        // Get current state
        Optional<ScaleCurrentState> currentStateOpt = currentStateRepository.findById(scale.getId());
        
        if (currentStateOpt.isEmpty()) {
            log.warn("[HEALTH-CHECK] Scale {} - NO CURRENT STATE DATA", scale.getId());
            result.hasIssue = true;
            result.healthStatus = ScaleHealthStatus.HealthStatus.NO_DATA;
            result.issueType = ScaleAlertEvent.IssueType.NO_SIGNAL;
            result.message = "No current state data found for scale";
            return result;
        }
        
        ScaleCurrentState currentState = currentStateOpt.get();
        log.debug("[HEALTH-CHECK] Scale {} - LastTime: {}, Status: {}", scale.getId(), currentState.getLastTime(), currentState.getStatus());
        
        // Check 1: Stale data (no recent updates)
        Duration timeSinceLastUpdate = Duration.between(currentState.getLastTime(), now);
        long minutesSinceUpdate = timeSinceLastUpdate.toMinutes();
        log.debug("[HEALTH-CHECK] Scale {} - Minutes since last update: {} (threshold: {})", 
            scale.getId(), minutesSinceUpdate, STALE_DATA_THRESHOLD_MINUTES);
        
        if (minutesSinceUpdate > STALE_DATA_THRESHOLD_MINUTES) {
            log.warn("[HEALTH-CHECK] Scale {} - DATA STALE: {} minutes old", scale.getId(), minutesSinceUpdate);
            result.hasIssue = true;
            result.healthStatus = ScaleHealthStatus.HealthStatus.NO_DATA;
            result.issueType = ScaleAlertEvent.IssueType.DATA_STALE;
            result.message = String.format("No data received for %d minutes", minutesSinceUpdate);
            result.lastUpdateTime = currentState.getLastTime();
            return result;
        }
        
        // Check 2: Status indicates error
        String status = currentState.getStatus();
        log.debug("[HEALTH-CHECK] Scale {} - Status check: '{}'", scale.getId(), status);
        if (status != null) {
            String statusLower = status.toLowerCase();
            if (statusLower.contains("error") || statusLower.contains("fail")) {
                log.warn("[HEALTH-CHECK] Scale {} - ERROR STATUS: {}", scale.getId(), status);
                result.hasIssue = true;
                result.healthStatus = ScaleHealthStatus.HealthStatus.READ_ERROR;
                result.issueType = ScaleAlertEvent.IssueType.READ_ERROR;
                result.message = "Scale status indicates error: " + status;
                result.currentStatus = status;
                return result;
            }
            
            if (statusLower.contains("offline") || statusLower.contains("disconnect")) {
                log.warn("[HEALTH-CHECK] Scale {} - OFFLINE STATUS: {}", scale.getId(), status);
                result.hasIssue = true;
                result.healthStatus = ScaleHealthStatus.HealthStatus.CONNECTION_LOST;
                result.issueType = ScaleAlertEvent.IssueType.CONNECTION_LOST;
                result.message = "Scale is offline or disconnected";
                result.currentStatus = status;
                return result;
            }
        }
        
        // Check 3: All data fields are zero (stopped state)
        log.debug("[HEALTH-CHECK] Scale {} - Data: data1={}, data2={}, data3={}, data4={}, data5={}", 
            scale.getId(), currentState.getData1(), currentState.getData2(), currentState.getData3(), 
            currentState.getData4(), currentState.getData5());
        
        if (isAllDataZero(currentState)) {
            log.warn("[HEALTH-CHECK] Scale {} - ALL DATA ZERO", scale.getId());
            result.hasIssue = true;
            result.healthStatus = ScaleHealthStatus.HealthStatus.STOPPED;
            result.issueType = ScaleAlertEvent.IssueType.ZERO_VALUE;
            result.message = "All data fields are zero (scale appears stopped)";
            result.lastKnownValue = "0";
            return result;
        }
        
        // All checks passed
        log.debug("[HEALTH-CHECK] Scale {} - HEALTHY", scale.getId());
        result.hasIssue = false;
        result.healthStatus = ScaleHealthStatus.HealthStatus.HEALTHY;
        result.message = "Scale is operating normally";
        result.currentStatus = currentState.getStatus();
        
        return result;
    }

    /**
     * Check if all data fields are zero
     */
    private boolean isAllDataZero(ScaleCurrentState state) {
        return isDataValueZero(state.getData1()) &&
               isDataValueZero(state.getData2()) &&
               isDataValueZero(state.getData3()) &&
               isDataValueZero(state.getData4()) &&
               isDataValueZero(state.getData5());
    }

    /**
     * Check if a data field value is zero
     */
    private boolean isDataValueZero(String dataJson) {
        if (dataJson == null || dataJson.isEmpty()) {
            return true;
        }
        
        try {
            // Simple check for common zero representations
            String normalized = dataJson.trim().replace("\"", "");
            return normalized.equals("0") || normalized.equals("0.0") || 
                   normalized.contains("\"value\":\"0\"") || normalized.contains("\"value\":0");
        } catch (Exception e) {
            log.debug("Error checking zero value: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Handle detected scale issue
     * Returns alert if it should be sent
     */
    private ScaleAlertEvent handleScaleIssue(Scale scale, ScaleHealthResult result, OffsetDateTime now) {
        log.debug("[HEALTH-CHECK] Scale {} - Handling issue: {}", scale.getId(), result.issueType);
        
        // Check if there's already an active issue
        Optional<ScaleHealthStatus> existingIssueOpt = 
            healthStatusRepository.findLatestActiveIssueByScaleId(scale.getId());
        
        if (existingIssueOpt.isPresent()) {
            // Update existing issue
            ScaleHealthStatus existingIssue = existingIssueOpt.get();
            int newFailureCount = existingIssue.getConsecutiveFailures() + 1;
            existingIssue.setConsecutiveFailures(newFailureCount);
            existingIssue.setDurationSeconds(Duration.between(existingIssue.getDetectedAt(), now).getSeconds());
            healthStatusRepository.save(existingIssue);
            
            log.debug("[HEALTH-CHECK] Scale {} - Consecutive failures: {} (threshold: {})", 
                scale.getId(), newFailureCount, CONSECUTIVE_FAILURES_ALERT_THRESHOLD);
            
            // Send alert if threshold reached
            if (newFailureCount >= CONSECUTIVE_FAILURES_ALERT_THRESHOLD) {
                log.info("[HEALTH-CHECK] Scale {} - ALERT TRIGGERED (failures: {})", scale.getId(), newFailureCount);
                return buildAlert(scale, existingIssue, result, false);
            }
            return null;
        } else {
            // Create new health status record
            log.info("[HEALTH-CHECK] Scale {} - NEW ISSUE DETECTED: {} ({})", 
                scale.getId(), result.issueType, result.message);
            
            ScaleHealthStatus newIssue = ScaleHealthStatus.builder()
                .scale(scale)
                .healthStatus(result.healthStatus)
                .issueType(convertToHealthIssueType(result.issueType))
                .detectedAt(now)
                .isActiveIssue(true)
                .consecutiveFailures(1)
                .lastKnownValue(result.lastKnownValue)
                .errorMessage(result.message)
                .build();
            
            healthStatusRepository.save(newIssue);
            log.debug("[HEALTH-CHECK] Scale {} - Issue saved, consecutive failures: 1 (threshold: {})", 
                scale.getId(), CONSECUTIVE_FAILURES_ALERT_THRESHOLD);
            
            // Check if we should alert immediately (when threshold is 1)
            if (CONSECUTIVE_FAILURES_ALERT_THRESHOLD == 1) {
                log.info("[HEALTH-CHECK] Scale {} - ALERT TRIGGERED IMMEDIATELY", scale.getId());
                return buildAlert(scale, newIssue, result, false);
            }
            return null; // Don't send alert on first detection (when threshold > 1)
        }
    }

    /**
     * Resolve existing issues if scale is now healthy
     * Returns list of recovery alerts
     */
    private List<ScaleAlertEvent> resolveExistingIssues(Scale scale, OffsetDateTime now) {
        List<ScaleAlertEvent> recoveryAlerts = new ArrayList<>();
        List<ScaleHealthStatus> activeIssues = 
            healthStatusRepository.findByScaleIdAndIsActiveIssueTrue(scale.getId());
        
        for (ScaleHealthStatus issue : activeIssues) {
            issue.setIsActiveIssue(false);
            issue.setResolvedAt(now);
            issue.setDurationSeconds(Duration.between(issue.getDetectedAt(), now).getSeconds());
            healthStatusRepository.save(issue);
            
            // Build recovery alert
            ScaleAlertEvent recoveryAlert = buildRecoveryAlert(scale, issue);
            recoveryAlerts.add(recoveryAlert);
        }
        
        return recoveryAlerts;
    }

    /**
     * Build alert event
     */
    private ScaleAlertEvent buildAlert(Scale scale, ScaleHealthStatus healthStatus, 
                                      ScaleHealthResult result, boolean isRecovery) {
        return ScaleAlertEvent.builder()
            .alertId(healthStatus.getId())
            .scale(buildScaleInfo(scale))
            .alertType(determineAlertType(healthStatus))
            .issueType(result.issueType)
            .detectedAt(healthStatus.getDetectedAt())
            .triggeredAt(OffsetDateTime.now())
            .durationSeconds(healthStatus.getDurationSeconds())
            .message(result.message)
            .consecutiveFailures(healthStatus.getConsecutiveFailures())
            .lastKnownValue(result.lastKnownValue)
            .currentStatus(result.currentStatus)
            .isRecovery(isRecovery)
            .build();
    }

    /**
     * Build recovery alert
     */
    private ScaleAlertEvent buildRecoveryAlert(Scale scale, ScaleHealthStatus healthStatus) {
        return ScaleAlertEvent.builder()
            .alertId(healthStatus.getId())
            .scale(buildScaleInfo(scale))
            .alertType(ScaleAlertEvent.AlertType.RECOVERY)
            .issueType(convertToAlertIssueType(healthStatus.getIssueType()))
            .detectedAt(healthStatus.getDetectedAt())
            .triggeredAt(OffsetDateTime.now())
            .durationSeconds(healthStatus.getDurationSeconds())
            .message("Scale recovered from " + healthStatus.getHealthStatus())
            .isRecovery(true)
            .build();
    }

    /**
     * Build scale info for alert
     */
    private ScaleAlertEvent.ScaleInfo buildScaleInfo(Scale scale) {
        String ipAddress = null;
        Integer port = null;
        
        // Extract IP and port from ScaleConfig if available
        if (scale.getConfig() != null && scale.getConfig().getConnParams() != null) {
            Map<String, Object> connParams = scale.getConfig().getConnParams();
            ipAddress = connParams.get("ip") != null ? connParams.get("ip").toString() : null;
            Object portObj = connParams.get("port");
            if (portObj != null) {
                port = portObj instanceof Integer ? (Integer) portObj : 
                       Integer.parseInt(portObj.toString());
            }
        }
        
        return ScaleAlertEvent.ScaleInfo.builder()
            .id(scale.getId())
            .name(scale.getName())
            .model(scale.getModel())
            .ipAddress(ipAddress)
            .port(port)
            .locationName(scale.getLocation() != null ? scale.getLocation().getName() : null)
            .locationId(scale.getLocation() != null ? scale.getLocation().getId() : null)
            .build();
    }

    /**
     * Determine alert type based on health status
     */
    private ScaleAlertEvent.AlertType determineAlertType(ScaleHealthStatus healthStatus) {
        int failures = healthStatus.getConsecutiveFailures();
        
        if (failures >= 10) {
            return ScaleAlertEvent.AlertType.CRITICAL;
        } else if (failures >= CONSECUTIVE_FAILURES_ALERT_THRESHOLD) {
            return ScaleAlertEvent.AlertType.WARNING;
        } else {
            return ScaleAlertEvent.AlertType.INFO;
        }
    }

    /**
     * Convert issue type
     */
    private ScaleHealthStatus.IssueType convertToHealthIssueType(ScaleAlertEvent.IssueType alertIssueType) {
        return switch (alertIssueType) {
            case ZERO_VALUE -> ScaleHealthStatus.IssueType.ZERO_VALUE;
            case NO_SIGNAL -> ScaleHealthStatus.IssueType.NO_SIGNAL;
            case CONNECTION_LOST, NETWORK_ERROR -> ScaleHealthStatus.IssueType.NETWORK_ERROR;
            case TIMEOUT -> ScaleHealthStatus.IssueType.TIMEOUT;
            case READ_ERROR, PROTOCOL_ERROR -> ScaleHealthStatus.IssueType.PROTOCOL_ERROR;
            case HARDWARE_ERROR -> ScaleHealthStatus.IssueType.HARDWARE_ERROR;
            default -> ScaleHealthStatus.IssueType.UNKNOWN;
        };
    }

    /**
     * Convert issue type from health status to alert
     */
    private ScaleAlertEvent.IssueType convertToAlertIssueType(ScaleHealthStatus.IssueType issueType) {
        if (issueType == null) return ScaleAlertEvent.IssueType.UNKNOWN;
        
        return switch (issueType) {
            case ZERO_VALUE -> ScaleAlertEvent.IssueType.ZERO_VALUE;
            case NO_SIGNAL -> ScaleAlertEvent.IssueType.NO_SIGNAL;
            case TIMEOUT -> ScaleAlertEvent.IssueType.TIMEOUT;
            case PROTOCOL_ERROR -> ScaleAlertEvent.IssueType.PROTOCOL_ERROR;
            case HARDWARE_ERROR -> ScaleAlertEvent.IssueType.HARDWARE_ERROR;
            case NETWORK_ERROR -> ScaleAlertEvent.IssueType.NETWORK_ERROR;
            default -> ScaleAlertEvent.IssueType.UNKNOWN;
        };
    }

    /**
     * Get monitoring statistics
     */
    public MonitoringStatistics getMonitoringStatistics() {
        long totalScales = scaleRepository.countByIsActive(true);
        long activeIssues = healthStatusRepository.countByIsActiveIssueTrue();
        
        List<Object[]> issuesByStatus = healthStatusRepository.countActiveIssuesByHealthStatus();
        
        MonitoringStatistics stats = new MonitoringStatistics();
        stats.totalActiveScales = totalScales;
        stats.totalActiveIssues = activeIssues;
        stats.healthyScales = totalScales - activeIssues;
        
        for (Object[] row : issuesByStatus) {
            ScaleHealthStatus.HealthStatus status = (ScaleHealthStatus.HealthStatus) row[0];
            Long count = (Long) row[1];
            
            switch (status) {
                case STOPPED -> stats.stoppedScales = count.intValue();
                case NO_DATA -> stats.noDataScales = count.intValue();
                case CONNECTION_LOST -> stats.disconnectedScales = count.intValue();
                case READ_ERROR -> stats.errorScales = count.intValue();
                default -> {}
            }
        }
        
        return stats;
    }

    /**
     * Get all active issues
     */
    public List<ScaleHealthStatus> getAllActiveIssues() {
        return healthStatusRepository.findByIsActiveIssueTrueOrderByDetectedAtDesc();
    }

    /**
     * Get active issues for a scale
     */
    public List<ScaleHealthStatus> getActiveIssues(Long scaleId) {
        return healthStatusRepository.findByScaleIdAndIsActiveIssueTrue(scaleId);
    }

    // DTOs
    
    public static class MonitoringStatistics {
        public long totalActiveScales;
        public long totalActiveIssues;
        public long healthyScales;
        public int stoppedScales;
        public int noDataScales;
        public int disconnectedScales;
        public int errorScales;
    }

    public static class ScaleHealthResult {
        public Long scaleId;
        public String scaleName;
        public OffsetDateTime checkTime;
        public boolean hasIssue;
        public ScaleHealthStatus.HealthStatus healthStatus;
        public ScaleAlertEvent.IssueType issueType;
        public String message;
        public String lastKnownValue;
        public String currentStatus;
        public OffsetDateTime lastUpdateTime;
    }

    /**
     * Internal result class for health check with alert info
     */
    private static class ScaleHealthCheckResult {
        public boolean hasIssue;
        public boolean hasRecovery;
        public boolean alertTriggered;
        public ScaleAlertEvent alert;
    }
}
