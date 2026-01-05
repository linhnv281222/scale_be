package org.facenet.entity.protocol;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

/**
 * Entity for protocols table
 * Quản lý các giao thức truyền thông (Modbus TCP, Modbus RTU, S-Bus, ...)
 */
@Entity
@Table(name = "protocols", indexes = {
    @Index(name = "idx_protocols_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Protocol extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "connection_type", length = 50)
    private String connectionType; // TCP, SERIAL, USB, etc.

    @Column(name = "default_port")
    private Integer defaultPort; // Default port for TCP protocols

    @Column(name = "default_baud_rate")
    private Integer defaultBaudRate; // Default baud rate for serial protocols

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "config_template", columnDefinition = "TEXT")
    private String configTemplate; // JSON template for configuration
}
