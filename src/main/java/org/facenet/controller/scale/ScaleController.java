package org.facenet.controller.scale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.scale.ScaleConfigDto;
import org.facenet.dto.scale.ScaleDto;
import org.facenet.dto.scale.WeighingDataDto;
import org.facenet.service.scale.WeighingLogService;
import org.facenet.service.scale.ScaleService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/scales")
@RequiredArgsConstructor
public class ScaleController {

    private final ScaleService scaleService;
    private final WeighingLogService weighingLogService;

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
    @Operation(summary = "Update scale configuration", 
               description = "Update the configuration for a specific scale including protocol, polling interval, and data mappings")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configuration updated successfully",
                           content = @Content(mediaType = "application/json", 
                                            schema = @Schema(implementation = ScaleConfigDto.Response.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scale not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<ScaleConfigDto.Response>> updateScaleConfig(
            @Parameter(description = "Scale ID", required = true, example = "1")
            @PathVariable("id") Long id,
            @Parameter(description = "Scale configuration data", required = true)
            @Valid @RequestBody ScaleConfigDto.Request request) {
        ScaleConfigDto.Response config = scaleService.updateScaleConfig(id, request);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    /**
     * Get weighing log history with pagination and optional filters.
     * Query params:
     * - scaleId: filter by scale
     * - startTime, endTime: filter by time range (ISO-8601)
     * - page, size: pagination
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<Page<WeighingDataDto.LogResponse>>> getScaleHistory(
            @RequestParam(name = "scaleId", required = false) Long scaleId,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        WeighingDataDto.LogQueryRequest request = WeighingDataDto.LogQueryRequest.builder()
                .scaleId(scaleId)
                .startTime(startTime)
                .endTime(endTime)
                .page(page)
                .size(size)
                .build();

        Page<WeighingDataDto.LogResponse> history = weighingLogService.getHistory(request);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}