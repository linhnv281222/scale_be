package org.facenet.entity.manufacturer;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

/**
 * Entity for scale_manufacturers table
 * Stores information about scale manufacturers/brands
 */
@Entity
@Table(name = "scale_manufacturers", indexes = {
    @Index(name = "idx_manufacturers_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScaleManufacturer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
