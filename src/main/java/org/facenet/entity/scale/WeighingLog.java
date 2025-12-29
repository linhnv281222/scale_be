package org.facenet.entity.scale;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.entity.shift.Shift;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entity for weighing_logs table (partitioned by created_at)
 * Historical data for scale measurements
 * Primary key is composite: (scale_id, created_at)
 * Note: Does not extend Auditable because createdAt is part of the primary key
 */
@Entity
@Table(name = "weighing_logs", indexes = {
    @Index(name = "idx_logs_scale_time", columnList = "scale_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(WeighingLog.WeighingLogId.class)
public class WeighingLog {

    @Id
    @Column(name = "scale_id")
    private Long scaleId;

    @Id
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scale_id", insertable = false, updatable = false)
    private Scale scale;

    @Column(name = "last_time", nullable = false)
    private OffsetDateTime lastTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

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

    // Audit fields (manually managed since this entity doesn't extend Auditable)
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        updatedAt = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Composite primary key class
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeighingLogId implements Serializable {
        private Long scaleId;
        private OffsetDateTime createdAt;
    }
}
