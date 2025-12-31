package org.facenet.entity.scale;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * Entity for scale_configs table
 * Uses JSONB for flexible configuration storage
 * @DynamicUpdate ensures only changed columns are included in UPDATE statements
 */
@Entity
@Table(name = "scale_configs")
@DynamicUpdate  // Critical for JSONB dirty checking with @MapsId
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScaleConfig extends Auditable {

    @Id
    @Column(name = "scale_id")
    private Long scaleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "scale_id")
    private Scale scale;

    @Column(name = "protocol", nullable = false, length = 20)
    private String protocol;

    @Column(name = "poll_interval")
    @Builder.Default
    private Integer pollInterval = 1000;

    @Type(JsonBinaryType.class)
    @Column(name = "conn_params", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> connParams;

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
}
