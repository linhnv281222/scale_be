package org.facenet.entity.report;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

/**
 * Report column entity
 * Defines individual columns in report
 */
@Entity
@Table(name = "report_columns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReportColumn extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    // Column definition
    @Column(name = "column_key", length = 50, nullable = false)
    private String columnKey;

    @Column(name = "column_label", length = 200, nullable = false)
    private String columnLabel;

    @Column(name = "column_order", nullable = false)
    private Integer columnOrder;

    // Data mapping
    @Column(name = "data_source", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private DataSource dataSource;

    @Column(name = "data_field", length = 50)
    private String dataField;

    @Column(name = "data_type", length = 20)
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    // Formatting
    @Column(name = "format_pattern", length = 100)
    private String formatPattern;

    @Column(name = "aggregation_type", length = 20)
    @Enumerated(EnumType.STRING)
    private AggregationType aggregationType;

    // Display
    @Column(name = "is_visible")
    private Boolean isVisible = true;

    @Column(name = "width")
    private Integer width;

    @Column(name = "alignment", length = 10)
    @Enumerated(EnumType.STRING)
    private Alignment alignment;

    public enum DataSource {
        SCALE_INFO, WEIGHING_DATA, CALCULATED
    }

    public enum DataType {
        STRING, NUMBER, DATE, BOOLEAN
    }

    public enum AggregationType {
        SUM, AVG, MAX, MIN, COUNT, NONE
    }

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }
}
