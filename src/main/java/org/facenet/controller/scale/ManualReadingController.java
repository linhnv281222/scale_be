package org.facenet.controller.scale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.scale.ManualReadingRequestDto;
import org.facenet.dto.scale.ManualReadingResponseDto;
import org.facenet.service.scale.ManualReadingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for manual reading operations
 * Handles on-demand data reading from scales
 */
@Slf4j
@RestController
@RequestMapping("/scales/manual-reading")
@RequiredArgsConstructor
@Tag(name = "Manual Reading", description = "APIs for manual/on-demand reading from scales")
public class ManualReadingController {
    
    private final ManualReadingService manualReadingService;
    
    /**
     * Perform manual reading from scales
     * 
     * Use cases:
     * - Read data at shift start (SHIFT_START)
     * - Read data at shift end (SHIFT_END)
     * - Read data on-demand (ON_DEMAND)
     * 
     * @param request Manual reading request
     * @return Reading results with success/failure details
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'OPERATOR')")
    @Operation(
        summary = "Perform manual reading",
        description = "Trigger manual data reading from scales. " +
                     "Can specify reading type (SHIFT_START, SHIFT_END, ON_DEMAND) and optional scale IDs. " +
                     "If no scale IDs provided, reads all active scales."
    )
    public ResponseEntity<ApiResponse<ManualReadingResponseDto>> performManualReading(
            @Valid @RequestBody ManualReadingRequestDto request) {
        
        log.info("[MANUAL-READING-API] Received manual reading request: {}", request);
        
        try {
            ManualReadingResponseDto response = manualReadingService.performManualReading(request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("[MANUAL-READING-API] Error performing manual reading: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to perform manual reading: " + e.getMessage()));
        }
    }
    
    /**
     * Read current data from a specific scale
     * 
     * @param scaleId Scale ID to read from
     * @return Current data values
     */
    @GetMapping("/{scaleId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(
        summary = "Read current data from specific scale",
        description = "Get current data values from scale's current state. " +
                     "Does not save to weighing log."
    )
    public ResponseEntity<ApiResponse<ManualReadingResponseDto.DataValues>> readScaleData(
            @PathVariable Long scaleId) {
        
        log.info("[MANUAL-READING-API] Reading data from scale {}", scaleId);
        
        try {
            ManualReadingResponseDto.DataValues dataValues = manualReadingService.readScaleData(scaleId);
            
            if (dataValues == null) {
                return ResponseEntity.ok(ApiResponse.error("No data available for scale " + scaleId));
            }
            
            return ResponseEntity.ok(ApiResponse.success(dataValues));
        } catch (Exception e) {
            log.error("[MANUAL-READING-API] Error reading scale data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to read scale data: " + e.getMessage()));
        }
    }
}
