package org.facenet.entity.report;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * Organization settings entity
 * Stores company information and branding
 */
@Entity
@Table(name = "organization_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class OrganizationSettings extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Company info
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_name_en")
    private String companyNameEn;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "website")
    private String website;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    // Branding
    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "logo_data")
    @Lob
    private byte[] logoData;

    @Column(name = "favicon_url", columnDefinition = "TEXT")
    private String faviconUrl;

    @Column(name = "favicon_data")
    @Lob
    private byte[] faviconData;

    @Column(name = "watermark_text", length = 100)
    private String watermarkText;

    // Status
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}
