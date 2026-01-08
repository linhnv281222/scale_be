package org.facenet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Batch alert event - sent when multiple scales have issues
 * More efficient than sending individual alerts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleBatchAlertEvent {

    /**
     * Timestamp when batch was created
     */
    private OffsetDateTime timestamp;

    /**
     * Number of scales with issues in this batch
     */
    private int totalIssues;

    /**
     * List of individual scale alerts
     */
    private List<ScaleAlertEvent> alerts;

    /**
     * Summary statistics
     */
    private AlertSummary summary;

    /**
     * Whether this batch contains recovery alerts
     */
    @Builder.Default
    private Boolean isRecoveryBatch = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertSummary {
        private int criticalCount;
        private int warningCount;
        private int infoCount;
        private int recoveryCount;
        
        private int zeroValueCount;
        private int noSignalCount;
        private int connectionLostCount;
        private int readErrorCount;
        private int staleDataCount;
        private int otherCount;
    }
}
