package org.facenet.entity.scale;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entity for scale_daily_reports table
 * Daily aggregated data per scale
 * Primary key is composite: (date, scale_id)
 */
@Entity
@Table(name = "scale_daily_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ScaleDailyReport.ScaleDailyReportId.class)
public class ScaleDailyReport extends Auditable {

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "scale_id")
    private Long scaleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scale_id", insertable = false, updatable = false)
    private Scale scale;

    @Type(JsonBinaryType.class)
    @Column(name = "data_1", columnDefinition = "jsonb")
    private Map<String, Object> data1;

    @Type(JsonBinaryType.class)
    @Column(name = "data_2", columnDefinition = "jsonb")
    private Map<String, Object> data2;

    @Type(JsonBinaryType.class)
    @Column(name = "data_3", columnDefinition = "jsonb")
    private Map<String, Object> data3;

    @Type(JsonBinaryType.class)
    @Column(name = "data_4", columnDefinition = "jsonb")
    private Map<String, Object> data4;

    @Type(JsonBinaryType.class)
    @Column(name = "data_5", columnDefinition = "jsonb")
    private Map<String, Object> data5;

    @Column(name = "last_time", nullable = false)
    private OffsetDateTime lastTime;

    /**
     * Composite primary key class
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaleDailyReportId implements Serializable {
        private LocalDate date;
        private Long scaleId;
    }
}
