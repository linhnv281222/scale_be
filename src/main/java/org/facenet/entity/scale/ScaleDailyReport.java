package org.facenet.entity.scale;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

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

    @Column(name = "data_1")
    private String data1;

    @Column(name = "data_2")
    private String data2;

    @Column(name = "data_3")
    private String data3;

    @Column(name = "data_4")
    private String data4;

    @Column(name = "data_5")
    private String data5;

    @Column(name = "last_time")
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
