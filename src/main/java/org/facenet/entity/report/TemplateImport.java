package org.facenet.entity.report;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

import java.time.OffsetDateTime;

/**
 * Template Import entity
 * Tracks imported template files with metadata
 * Stores file location path in resources directory
 */
@Entity
@Table(name = "template_imports", indexes = {
    @Index(name = "idx_template_code", columnList = "template_code"),
    @Index(name = "idx_import_status", columnList = "import_status"),
    @Index(name = "idx_template_import_id", columnList = "template_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class TemplateImport extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    @Column(name = "template_code", length = 50, nullable = false)
    private String templateCode;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    /**
     * Relative path in resources (e.g., templates/reports/my-template.docx)
     */
    @Column(name = "resource_path", length = 500, nullable = false, unique = true)
    private String resourcePath;

    /**
     * Absolute file system path where template is stored
     */
    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_hash", length = 64)
    private String fileHash; // SHA-256 hash for integrity check

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", length = 20, nullable = false)
    @Builder.Default
    private ImportStatus importStatus = ImportStatus.ACTIVE;

    @Column(name = "import_date", nullable = false)
    private OffsetDateTime importDate;

    @Column(name = "import_notes", columnDefinition = "TEXT")
    private String importNotes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", length = 50)
    private TemplateType templateType;

    public enum ImportStatus {
        PENDING,      // File uploaded, waiting for processing
        ACTIVE,       // Successfully imported and in use
        ARCHIVED,     // No longer used but kept for history
        CORRUPTED,    // File integrity check failed
        DELETED       // Marked for deletion
    }

    public enum TemplateType {
        SHIFT_REPORT("Báo cáo ca"),
        WEIGHING_REPORT("Báo cáo cân"),
        WEIGHING_SLIP("Phiếu cân");

        private final String displayName;

        TemplateType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static TemplateType fromDisplayName(String displayName) {
            if (displayName == null || displayName.trim().isEmpty()) {
                return null;
            }
            // Try exact match with display name first
            for (TemplateType type : values()) {
                if (type.displayName.equalsIgnoreCase(displayName.trim())) {
                    return type;
                }
            }
            // Try match with enum name
            try {
                return TemplateType.valueOf(displayName.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
