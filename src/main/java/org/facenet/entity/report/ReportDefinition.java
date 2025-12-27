package org.facenet.entity.report;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * Report Definition Entity
 * Defines the structure and metadata of enterprise reports
 * Reports must be defined before they can be executed
 */
@Entity
@Table(name = "report_definitions", indexes = {
    @Index(name = "idx_report_code", columnList = "report_code", unique = true),
    @Index(name = "idx_report_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReportDefinition extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique report code (e.g., BCSL, BCCN, BCCT)
     * Used for report identification and execution
     */
    @Column(name = "report_code", length = 50, nullable = false, unique = true)
    private String reportCode;

    /**
     * Human-readable report name
     */
    @Column(name = "report_name", length = 255, nullable = false)
    private String reportName;

    /**
     * Detailed description of the report
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Data source for the report
     * Options: weighing_logs, scale_daily_reports, custom query
     */
    @Column(name = "data_source", length = 100, nullable = false)
    private String dataSource;

    /**
     * Supported export formats (EXCEL, WORD, PDF)
     * Stored as JSON array
     */
    @Type(JsonBinaryType.class)
    @Column(name = "supported_formats", columnDefinition = "jsonb", nullable = false)
    private String[] supportedFormats;

    /**
     * Parameter schema (JSON)
     * Defines required and optional parameters for the report
     */
    @Type(JsonBinaryType.class)
    @Column(name = "parameter_schema", columnDefinition = "jsonb")
    private Map<String, Object> parameterSchema;

    /**
     * Required permission to execute this report
     */
    @Column(name = "required_permission", length = 100)
    private String requiredPermission;

    /**
     * Report status
     */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.ACTIVE;

    /**
     * Report version (for versioning and compatibility)
     */
    @Column(name = "version", length = 20)
    private String version;

    /**
     * Layout type (ENTERPRISE_STANDARD, COMPACT, DETAILED)
     */
    @Column(name = "layout_type", length = 50)
    @Builder.Default
    private String layoutType = "ENTERPRISE_STANDARD";

    /**
     * Style profile (SCALEHUB_OFFICIAL, etc.)
     */
    @Column(name = "style_profile", length = 50)
    @Builder.Default
    private String styleProfile = "SCALEHUB_OFFICIAL";

    /**
     * Default template ID (optional)
     */
    @Column(name = "default_template_id")
    private Long defaultTemplateId;

    /**
     * Execution timeout in seconds
     */
    @Column(name = "execution_timeout_seconds")
    @Builder.Default
    private Integer executionTimeoutSeconds = 300;

    /**
     * Maximum records limit (for performance)
     */
    @Column(name = "max_records_limit")
    private Integer maxRecordsLimit;

    /**
     * Enable/disable audit logging for this report
     */
    @Column(name = "audit_enabled")
    @Builder.Default
    private Boolean auditEnabled = true;

    /**
     * Report category (for grouping)
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Display order (for UI sorting)
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Report status enum
     */
    public enum ReportStatus {
        ACTIVE,
        INACTIVE,
        DRAFT,
        DEPRECATED
    }

    /**
     * Check if report is executable
     */
    public boolean isExecutable() {
        return this.status == ReportStatus.ACTIVE;
    }

    /**
     * Check if format is supported
     */
    public boolean supportsFormat(String format) {
        if (supportedFormats == null) {
            return false;
        }
        for (String supportedFormat : supportedFormats) {
            if (supportedFormat.equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }
}
