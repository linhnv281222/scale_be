package org.facenet.entity.form;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

/**
 * Entity for form_templates table
 */
@Entity
@Table(name = "form_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormTemplate extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
}
