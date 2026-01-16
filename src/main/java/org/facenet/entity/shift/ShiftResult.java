package org.facenet.entity.shift;

import jakarta.persistence.*;
import lombok.*;
import org.facenet.common.audit.Auditable;
import org.facenet.entity.scale.Scale;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity for shift_results table
 * Stores shift results with start/end values and deviation for data_1
 */
@Entity
@Table(name = "shift_results", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_shift_result_scale_shift_date", 
                         columnNames = {"scale_id", "shift_id", "shift_date"})
    },
    indexes = {
        @Index(name = "idx_shift_results_scale_id", columnList = "scale_id"),
        @Index(name = "idx_shift_results_shift_id", columnList = "shift_id"),
        @Index(name = "idx_shift_results_shift_date", columnList = "shift_date"),
        @Index(name = "idx_shift_results_scale_shift_date", columnList = "scale_id, shift_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftResult extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scale_id", nullable = false)
    private Scale scale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_value_data1", length = 50)
    private String startValueData1;

    @Column(name = "end_value_data1", length = 50)
    private String endValueData1;

    @Column(name = "deviation_data1", precision = 15, scale = 2)
    private BigDecimal deviationData1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
