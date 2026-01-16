package org.facenet.service.scale;

import org.facenet.dto.scale.ManualReadingRequestDto;
import org.facenet.dto.scale.ManualReadingResponseDto;

/**
 * Service for manual reading operations
 * Handles on-demand data reading from scales, especially for shift start/end
 */
public interface ManualReadingService {
    
    /**
     * Perform manual reading from scales
     * 
     * @param request Manual reading request with type and optional scale IDs
     * @return Response with reading results
     */
    ManualReadingResponseDto performManualReading(ManualReadingRequestDto request);
    
    /**
     * Read current data from a specific scale
     * 
     * @param scaleId Scale ID to read from
     * @return Current data from scale, or null if read fails
     */
    ManualReadingResponseDto.DataValues readScaleData(Long scaleId);
}
