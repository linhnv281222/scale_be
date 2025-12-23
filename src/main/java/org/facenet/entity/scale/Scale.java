package org.facenet.entity.scale;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.facenet.entity.location.Location;

/**
 * Entity for scales table
 */
@Entity
@Table(name = "scales", indexes = {
    @Index(name = "idx_scales_location", columnList = "location_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scale extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToOne(mappedBy = "scale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ScaleConfig config;

    @OneToOne(mappedBy = "scale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ScaleCurrentState currentState;
}
