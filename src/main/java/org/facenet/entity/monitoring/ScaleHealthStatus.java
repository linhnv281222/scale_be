package org.facenet.entity.monitoring;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.facenet.entity.scale.Scale;

import java.time.OffsetDateTime;

/**
 * Entity to track scale health status and connection monitoring
 * Stores historical data about scale connectivity issues
 */
@Entity
@Table(name = "scale_health_status", indexes = {
    @Index(name = "idx_health_scale_id", columnList = "scale_id"),
    @Index(name = "idx_health_status", columnList = "health_status"),
    @Index(name = "idx_health_detected_at", columnList = "detected_at"),
    @Index(name = "idx_health_is_active", columnList = "is_active_issue")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScaleHealthStatus extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scale_id", nullable = false)
    private Scale scale;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 50, nullable = false)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", length = 50)
    private IssueType issueType;

    /**
     * When the issue was first detected
     */
    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    /**
     * When the issue was resolved (null if still active)
     */
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    /**
     * Duration of the issue in seconds
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /**
     * Whether this is an active/ongoing issue
     */
    @Column(name = "is_active_issue", nullable = false)
    @Builder.Default
    private Boolean isActiveIssue = true;

    /**
     * Last known value before issue (for stopped state)
     */
    @Column(name = "last_known_value")
    private String lastKnownValue;

    /**
     * Error message or description
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of consecutive failures
     */
    @Column(name = "consecutive_failures")
    @Builder.Default
    private Integer consecutiveFailures = 0;

    /**
     * Additional metadata (JSON)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public enum HealthStatus {
        HEALTHY,           // Cân hoạt động bình thường
        STOPPED,           // Cân dừng (giá trị = 0)
        NO_DATA,           // Không nhận được dữ liệu
        CONNECTION_LOST,   // Mất kết nối
        READ_ERROR,        // Lỗi khi đọc dữ liệu
        DEGRADED           // Hoạt động không ổn định
    }

    public enum IssueType {
        ZERO_VALUE,        // Giá trị luôn = 0
        NO_SIGNAL,         // Không có tín hiệu
        TIMEOUT,           // Timeout khi đọc
        PROTOCOL_ERROR,    // Lỗi giao thức
        HARDWARE_ERROR,    // Lỗi phần cứng
        NETWORK_ERROR,     // Lỗi mạng
        UNKNOWN            // Không xác định
    }
}
