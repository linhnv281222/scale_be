package org.facenet.entity.report;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Report Execution History Entity
 * Tracks all report executions with full parameter and result details
 */
@Entity
@Table(name = "report_execution_history", indexes = {
    @Index(name = "idx_execution_report", columnList = "report_code"),
    @Index(name = "idx_execution_time", columnList = "execution_start_time DESC"),
    @Index(name = "idx_execution_user", columnList = "executed_by,execution_start_time DESC"),
    @Index(name = "idx_execution_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReportExecutionHistory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to Report Definition
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_definition_id", nullable = false)
    private ReportDefinition reportDefinition;

    /**
     * Report code (denormalized for quick query)
     */
    @Column(name = "report_code", length = 50, nullable = false)
    private String reportCode;

    /**
     * Export format (EXCEL, WORD, PDF)
     */
    @Column(name = "export_format", length = 20, nullable = false)
    private String exportFormat;

    /**
     * Execution parameters (JSON)
     * Contains: scaleIds, startTime, endTime, etc.
     */
    @Type(JsonBinaryType.class)
    @Column(name = "parameters", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> parameters;

    /**
     * Execution start time
     */
    @Column(name = "execution_start_time", nullable = false)
    private OffsetDateTime executionStartTime;

    /**
     * Execution end time
     */
    @Column(name = "execution_end_time")
    private OffsetDateTime executionEndTime;

    /**
     * Execution time in milliseconds
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /**
     * Execution status
     */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    /**
     * User who executed the report
     */
    @Column(name = "executed_by", length = 100, nullable = false)
    private String executedBy;

    /**
     * Result metadata
     */
    @Type(JsonBinaryType.class)
    @Column(name = "result_metadata", columnDefinition = "jsonb")
    private Map<String, Object> resultMetadata;

    /**
     * Number of records in the report
     */
    @Column(name = "record_count")
    private Integer recordCount;

    /**
     * Generated file name
     */
    @Column(name = "file_name", length = 500)
    private String fileName;

    /**
     * File size in bytes
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * Error message (if failed)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Error stack trace (if failed)
     */
    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    /**
     * Client IP address
     */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /**
     * User agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Template ID used (if any)
     */
    @Column(name = "template_id")
    private Long templateId;

    /**
     * Execution status enum
     */
    public enum ExecutionStatus {
        STARTED,
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }

    /**
     * Calculate execution time from start and end times
     */
    public void calculateExecutionTime() {
        if (executionStartTime != null && executionEndTime != null) {
            this.executionTimeMs = java.time.Duration.between(executionStartTime, executionEndTime).toMillis();
        }
    }

    /**
     * Mark execution as successful
     */
    public void markSuccess(int recordCount, String fileName, long fileSizeBytes) {
        this.status = ExecutionStatus.SUCCESS;
        this.executionEndTime = OffsetDateTime.now();
        this.recordCount = recordCount;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        calculateExecutionTime();
    }

    /**
     * Mark execution as failed
     */
    public void markFailed(String errorMessage, String stackTrace) {
        this.status = ExecutionStatus.FAILED;
        this.executionEndTime = OffsetDateTime.now();
        this.errorMessage = errorMessage;
        this.errorStackTrace = stackTrace;
        calculateExecutionTime();
    }
}
