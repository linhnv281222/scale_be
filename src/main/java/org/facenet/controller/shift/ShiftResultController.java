package org.facenet.controller.shift;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.shift.ShiftResultResponseDto;
import org.facenet.dto.shift.ShiftResultUpdateRequestDto;
import org.facenet.service.shift.ShiftResultService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for shift result operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/shift-results")
@RequiredArgsConstructor
@Tag(name = "Shift Results", description = "APIs for managing shift results")
public class ShiftResultController {

    private final ShiftResultService shiftResultService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get shift result by ID")
    public ResponseEntity<ApiResponse<ShiftResultResponseDto>> getById(
            @Parameter(description = "Shift result ID") @PathVariable Long id) {
        log.info("[SHIFT-RESULT-API] Getting shift result by id: {}", id);
        ShiftResultResponseDto result = shiftResultService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scale/{scaleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get all shift results for a scale")
    public ResponseEntity<ApiResponse<List<ShiftResultResponseDto>>> getByScaleId(
            @Parameter(description = "Scale ID") @PathVariable Long scaleId) {
        log.info("[SHIFT-RESULT-API] Getting shift results for scale: {}", scaleId);
        List<ShiftResultResponseDto> results = shiftResultService.getByScaleId(scaleId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/scale/{scaleId}/shift/{shiftId}/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get shift result by scale, shift, and date")
    public ResponseEntity<ApiResponse<ShiftResultResponseDto>> getByScaleShiftAndDate(
            @Parameter(description = "Scale ID") @PathVariable Long scaleId,
            @Parameter(description = "Shift ID") @PathVariable Long shiftId,
            @Parameter(description = "Shift date (YYYY-MM-DD)") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("[SHIFT-RESULT-API] Getting shift result: scaleId={}, shiftId={}, date={}", scaleId, shiftId, date);
        ShiftResultResponseDto result = shiftResultService.getByScaleShiftAndDate(scaleId, shiftId, date);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get shift results by date range")
    public ResponseEntity<ApiResponse<List<ShiftResultResponseDto>>> getByDateRange(
            @Parameter(description = "From date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Scale ID (optional)") @RequestParam(required = false) Long scaleId) {
        
        if (scaleId != null) {
            log.info("[SHIFT-RESULT-API] Getting shift results for scale {} from {} to {}", scaleId, fromDate, toDate);
            List<ShiftResultResponseDto> results = shiftResultService.getByScaleIdAndDateRange(scaleId, fromDate, toDate);
            return ResponseEntity.ok(ApiResponse.success(results));
        } else {
            log.info("[SHIFT-RESULT-API] Getting shift results from {} to {}", fromDate, toDate);
            List<ShiftResultResponseDto> results = shiftResultService.getByDateRange(fromDate, toDate);
            return ResponseEntity.ok(ApiResponse.success(results));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update shift result")
    public ResponseEntity<ApiResponse<ShiftResultResponseDto>> update(
            @Parameter(description = "Shift result ID") @PathVariable Long id,
            @RequestBody ShiftResultUpdateRequestDto request) {
        log.info("[SHIFT-RESULT-API] Updating shift result: {}", id);
        ShiftResultResponseDto result = shiftResultService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete shift result (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Shift result ID") @PathVariable Long id) {
        log.info("[SHIFT-RESULT-API] Deleting shift result: {}", id);
        shiftResultService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Shift result deleted successfully"));
    }
}
