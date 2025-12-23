package org.facenet.entity.scale;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entity for scale_current_states table
 * Stores real-time state of scales
 * last_time = when device returned data (not when inserted to DB)
 */
@Entity
@Table(name = "scale_current_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScaleCurrentState extends Auditable {

    @Id
    @Column(name = "scale_id")
    private Long scaleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "scale_id")
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

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "last_time", nullable = false)
    private OffsetDateTime lastTime;
}
