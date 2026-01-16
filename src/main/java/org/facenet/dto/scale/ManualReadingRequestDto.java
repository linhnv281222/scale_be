package org.facenet.dto.scale;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for manual reading request
 * Used to trigger manual data reading at shift start/end or on-demand
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualReadingRequestDto {
    
    /**
     * Reading type: SHIFT_START, SHIFT_END, ON_DEMAND
     */
    @NotNull(message = "Reading type is required")
    private ReadingType readingType;
    
    /**
     * Optional: Specific scale IDs to read
     * If null or empty, reads all active scales
     */
    private List<Long> scaleIds;
    
    /**
     * Optional: Shift ID for shift-based readings
     * Used to mark the reading with specific shift
     */
    private Long shiftId;
    
    /**
     * Optional: Note/reason for manual reading
     */
    private String note;
    
    public enum ReadingType {
        SHIFT_START,    // Đầu ca
        SHIFT_END,      // Cuối ca
        ON_DEMAND       // Đọc theo yêu cầu
    }
}
