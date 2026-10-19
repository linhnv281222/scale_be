package org.facenet.entity.scale;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "scale_config_audit_logs", indexes = {
        @Index(name = "idx_scale_config_audit_scale_time", columnList = "scale_id, changed_at")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleConfigAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scale_id", nullable = false)
    private Long scaleId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Type(JsonBinaryType.class)
    @Column(name = "before_config", columnDefinition = "jsonb")
    private Map<String, Object> beforeConfig;

    @Type(JsonBinaryType.class)
    @Column(name = "after_config", columnDefinition = "jsonb")
    private Map<String, Object> afterConfig;

    @Column(name = "changed_by", nullable = false, length = 256)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;
}
