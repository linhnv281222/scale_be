package org.facenet.entity.shift;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;

import java.time.LocalTime;

@Entity
@Table(name = "shifts", indexes = {
        @Index(name = "idx_shifts_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "is_active")
    private Boolean isActive;
}
