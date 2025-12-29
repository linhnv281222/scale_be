package org.facenet.entity.report;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Report template entity
 * Defines dynamic report configuration
 */
@Entity
@Table(name = "report_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReportTemplate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "report_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(name = "title_template", length = 500)
    private String titleTemplate;

    // ===== WORD template file (DOCX) =====
    @Column(name = "word_template_filename", length = 255)
    private String wordTemplateFilename;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "word_template_content")
    private byte[] wordTemplateContent;

    // JSONB configurations
    @Type(JsonBinaryType.class)
    @Column(name = "header_config", columnDefinition = "jsonb")
    private Map<String, Object> headerConfig;

    @Type(JsonBinaryType.class)
    @Column(name = "footer_config", columnDefinition = "jsonb")
    private Map<String, Object> footerConfig;

    @Type(JsonBinaryType.class)
    @Column(name = "table_config", columnDefinition = "jsonb")
    private Map<String, Object> tableConfig;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    // Relationships
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("columnOrder ASC")
    @Builder.Default
    private List<ReportColumn> columns = new ArrayList<>();

    public enum ReportType {
        EXCEL, WORD, PDF
    }
}
