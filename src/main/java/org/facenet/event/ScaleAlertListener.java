package org.facenet.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for Scale Alert Events
 * Handles alert notifications, logging, and custom actions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScaleAlertListener {

    /**
     * Handle batch alert events (always sent as batch/array)
     */
    @EventListener
    @Async
    public void handleBatchAlert(ScaleBatchAlertEvent event) {
        if (event.getIsRecoveryBatch()) {
            handleRecoveryBatch(event);
        } else {
            handleIssueBatch(event);
        }
    }

    /**
     * Handle issue batch alerts
     */
    private void handleIssueBatch(ScaleBatchAlertEvent event) {
        log.warn("========================================");
        log.warn("[ALERT BATCH] {} scale(s) with issues detected!", event.getTotalIssues());
        log.warn("Timestamp: {}", event.getTimestamp());
        
        // Log summary
        ScaleBatchAlertEvent.AlertSummary summary = event.getSummary();
        if (summary != null) {
            log.warn("Summary:");
            log.warn("  - Critical: {}, Warning: {}, Info: {}", 
                summary.getCriticalCount(), summary.getWarningCount(), summary.getInfoCount());
            log.warn("  - Zero Value: {}, No Signal: {}, Connection Lost: {}", 
                summary.getZeroValueCount(), summary.getNoSignalCount(), summary.getConnectionLostCount());
            log.warn("  - Read Error: {}, Stale Data: {}, Other: {}", 
                summary.getReadErrorCount(), summary.getStaleDataCount(), summary.getOtherCount());
        }
        
        // Log each scale
        log.warn("Affected scales:");
        for (ScaleAlertEvent alert : event.getAlerts()) {
            log.warn("  - {} (ID: {}) - {} - {}", 
                alert.getScale().getName(), 
                alert.getScale().getId(),
                alert.getIssueType(),
                alert.getMessage());
        }
        log.warn("========================================");
        
        // TODO: Additional batch alert handling:
        // - Send consolidated email/SMS
        // - Create incident ticket
        // - Trigger escalation if too many critical issues
        // - Update dashboard
    }

    /**
     * Handle recovery batch alerts
     */
    private void handleRecoveryBatch(ScaleBatchAlertEvent event) {
        log.info("========================================");
        log.info("[RECOVERY BATCH] {} scale(s) recovered!", event.getTotalIssues());
        log.info("Timestamp: {}", event.getTimestamp());
        
        // Log each recovered scale
        log.info("Recovered scales:");
        for (ScaleAlertEvent alert : event.getAlerts()) {
            log.info("  - {} (ID: {}) - Downtime: {}s", 
                alert.getScale().getName(), 
                alert.getScale().getId(),
                alert.getDurationSeconds() != null ? alert.getDurationSeconds() : 0);
        }
        log.info("========================================");
        
        // TODO: Send recovery notifications
    }

    // Keep old handlers for backward compatibility or remove if not needed anymore
}
