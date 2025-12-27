package org.facenet.entity.report;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Report export log entity
 * Audit trail for report exports
 */
@Entity
@Table(name = "report_export_logs", indexes = {
    @Index(name = "idx_export_logs_created", columnList = "created_at"),
    @Index(name = "idx_export_logs_user", columnList = "created_by,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReportExportLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ReportTemplate template;

    // Request info
    @Column(name = "export_type", length = 20, nullable = false)
    private String exportType;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Type(JsonBinaryType.class)
    @Column(name = "scale_ids", columnDefinition = "jsonb")
    private List<Long> scaleIds;

    // Result
    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ExportStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Performance
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    public enum ExportStatus {
        SUCCESS, FAILED, TIMEOUT
    }
}
