package org.facenet.entity.location;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity for locations table
 * Supports hierarchical structure with parent_id
 */
@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_locations_parent", columnList = "parent_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 20)
    private String code;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Location> children = new ArrayList<>();
}
