package org.facenet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Event for scale alerts and monitoring notifications
 * Broadcasted when a scale has connectivity or data reading issues
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleAlertEvent {

    /**
     * Alert ID (for tracking)
     */
    private Long alertId;

    /**
     * Scale information
     */
    private ScaleInfo scale;

    /**
     * Alert type/severity
     */
    private AlertType alertType;

    /**
     * Issue type
     */
    private IssueType issueType;

    /**
     * When the issue was first detected
     */
    private OffsetDateTime detectedAt;

    /**
     * When the alert was triggered
     */
    private OffsetDateTime triggeredAt;

    /**
     * Duration of the issue in seconds (if known)
     */
    private Long durationSeconds;

    /**
     * Error message or description
     */
    private String message;

    /**
     * Number of consecutive failures
     */
    private Integer consecutiveFailures;

    /**
     * Last known value before issue
     */
    private String lastKnownValue;

    /**
     * Current status
     */
    private String currentStatus;

    /**
     * Additional metadata
     */
    private String metadata;

    /**
     * Whether this is a recovery alert (issue resolved)
     */
    @Builder.Default
    private Boolean isRecovery = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaleInfo {
        private Long id;
        private String name;
        private String model;
        private String ipAddress;
        private Integer port;
        private String locationName;
        private Long locationId;
    }

    public enum AlertType {
        CRITICAL,    // Nguy kịch - cần xử lý ngay
        WARNING,     // Cảnh báo
        INFO,        // Thông tin
        RECOVERY     // Phục hồi - vấn đề đã được giải quyết
    }

    public enum IssueType {
        ZERO_VALUE,        // Giá trị luôn = 0 (trạng thái dừng)
        NO_SIGNAL,         // Không có tín hiệu
        CONNECTION_LOST,   // Mất kết nối
        TIMEOUT,           // Timeout khi đọc
        READ_ERROR,        // Lỗi khi đọc dữ liệu
        PROTOCOL_ERROR,    // Lỗi giao thức
        HARDWARE_ERROR,    // Lỗi phần cứng
        NETWORK_ERROR,     // Lỗi mạng
        DATA_STALE,        // Dữ liệu cũ (không cập nhật)
        UNKNOWN            // Không xác định
    }
}
