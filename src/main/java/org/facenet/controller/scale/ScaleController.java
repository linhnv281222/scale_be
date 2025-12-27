package org.facenet.controller.scale;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.scale.ScaleConfigDto;
import org.facenet.dto.scale.ScaleDto;
import org.facenet.service.scale.ScaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scales")
@RequiredArgsConstructor
public class ScaleController {

    private final ScaleService scaleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<ScaleDto.Response>>> getAllScales(
            @RequestParam(value = "location_id", required = false) Long location_id,
            @RequestParam(value = "location_ids", required = false) List<Long> location_ids) {
        List<ScaleDto.Response> scales;
        
        // Priority: location_ids > location_id (for backward compatibility)
        if (location_ids != null && !location_ids.isEmpty()) {
            scales = scaleService.getScalesByLocations(location_ids);
        } else if (location_id != null) {
            scales = scaleService.getScalesByLocation(location_id);
        } else {
            scales = scaleService.getAllScales();
        }
        return ResponseEntity.ok(ApiResponse.success(scales));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ScaleDto.Response>> getScaleById(
            @PathVariable("id") Long id) {
        ScaleDto.Response scale = scaleService.getScaleById(id);
        return ResponseEntity.ok(ApiResponse.success(scale));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ScaleDto.Response>> createScale(
            @Valid @RequestBody ScaleDto.Request request) {
        ScaleDto.Response scale = scaleService.createScale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(scale));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ScaleDto.Response>> updateScale(
            @PathVariable("id") Long id,
            @Valid @RequestBody ScaleDto.Request request) {
        ScaleDto.Response scale = scaleService.updateScale(id, request);
        return ResponseEntity.ok(ApiResponse.success(scale));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteScale(
            @PathVariable("id") Long id) {
        scaleService.deleteScale(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ScaleConfigDto.Response>> getScaleConfig(
            @PathVariable("id") Long id) {
        ScaleConfigDto.Response config = scaleService.getScaleConfig(id);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PutMapping("/{id}/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ScaleConfigDto.Response>> updateScaleConfig(
            @PathVariable("id") Long id,
            @Valid @RequestBody ScaleConfigDto.Request request) {
        ScaleConfigDto.Response config = scaleService.updateScaleConfig(id, request);
        return ResponseEntity.ok(ApiResponse.success(config));
    }
}