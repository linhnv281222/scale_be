package org.facenet.controller.scale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.scale.ScaleDto;
import org.facenet.dto.scale.ScaleStatsResponseDto;
import org.facenet.service.scale.ScaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Scale Management
 */
@RestController
@RequestMapping("/scales")
@RequiredArgsConstructor
@Tag(name = "Scale Management", description = "APIs for managing scales with integrated configuration")
public class ScaleController {

    private final ScaleService scaleService;

    /**
     * Get all scales with pagination and filters
     * Supports search on name, model
     * Supports filters: locationId, manufacturerId, protocolId, direction, isActive, model
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(
        summary = "Get all scales",
        description = "Retrieve paginated list of scales with optional search and filters. " +
                     "Search: name, model. Filters: locationId, manufacturerId, protocolId, direction, isActive, model"
    )
    public ResponseEntity<ApiResponse<?>> getAllScales(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "locationId", required = false) Long locationId,
            @RequestParam(value = "manufacturerId", required = false) Long manufacturerId,
            @RequestParam(value = "protocolId", required = false) Long protocolId,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        
        Map<String, String> filters = new java.util.HashMap<>();
        if (locationId != null) {
            filters.put("location.id", String.valueOf(locationId));
        }
        if (manufacturerId != null) {
            filters.put("manufacturer.id", String.valueOf(manufacturerId));
        }
        if (protocolId != null) {
            filters.put("protocol.id", String.valueOf(protocolId));
        }
        if (model != null) {
            filters.put("model", model);
        }
        if (direction != null) {
            filters.put("direction", direction);
        }
        if (isActive != null) {
            filters.put("isActive", String.valueOf(isActive));
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<ScaleDto.Response> response = scaleService.getAllScales(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all scales with pagination, filters and statistics (V2)
     * Supports search on name, model
     * Supports filters: locationId (array), manufacturerId (array), protocolId (array), direction, isActive, model
     * Returns: paginated data with total_scales, active_scales, inactive_scales
     */
    @GetMapping("/v2")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(
        summary = "Get all scales with statistics (V2)",
        description = "Retrieve paginated list of scales with optional search and filters, including statistics. " +
                     "Search: name, model. Filters: locationId (supports multiple), manufacturerId (supports multiple), protocolId (supports multiple), direction, isActive, model. " +
                     "Response includes: total_scales, active_scales, inactive_scales"
    )
    public ResponseEntity<ApiResponse<?>> getAllScalesV2(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "locationId", required = false) java.util.List<Long> locationIds,
            @RequestParam(value = "manufacturerId", required = false) java.util.List<Long> manufacturerIds,
            @RequestParam(value = "protocolId", required = false) java.util.List<Long> protocolIds,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        
        Map<String, String> filters = new java.util.HashMap<>();
        if (locationIds != null && !locationIds.isEmpty()) {
            filters.put("location.id_in", locationIds.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",")));
        }
        if (manufacturerIds != null && !manufacturerIds.isEmpty()) {
            filters.put("manufacturer.id_in", manufacturerIds.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",")));
        }
        if (protocolIds != null && !protocolIds.isEmpty()) {
            filters.put("protocol.id_in", protocolIds.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",")));
        }
        if (model != null) {
            filters.put("model", model);
        }
        if (direction != null) {
            filters.put("direction", direction);
        }
        if (isActive != null) {
            filters.put("isActive", String.valueOf(isActive));
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        ScaleStatsResponseDto response = scaleService.getAllScalesV2(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get scale by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(
        summary = "Get scale by ID",
        description = "Retrieve a specific scale with its configuration by ID"
    )
    public ResponseEntity<ScaleDto.Response> getScaleById(@PathVariable Long id) {
        ScaleDto.Response response = scaleService.getScaleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new scale with configuration
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Create new scale",
        description = "Create a new scale with integrated configuration. All configuration fields can be provided in the request."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Scale creation request with configuration",
        required = true,
        content = @Content(schema = @Schema(implementation = ScaleDto.Request.class))
    )
    public ResponseEntity<ScaleDto.Response> createScale(@Valid @RequestBody ScaleDto.Request request) {
        ScaleDto.Response response = scaleService.createScale(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update scale with configuration
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Update scale",
        description = "Update an existing scale with integrated configuration. All fields including configuration can be updated."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Scale update request with configuration",
        required = true,
        content = @Content(schema = @Schema(implementation = ScaleDto.Request.class))
    )
    public ResponseEntity<ScaleDto.Response> updateScale(
            @PathVariable Long id,
            @Valid @RequestBody ScaleDto.Request request) {
        ScaleDto.Response response = scaleService.updateScale(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete scale (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete scale (soft delete)",
        description = "Soft delete a scale by ID (sets is_active to false). Scale can be reactivated later."
    )
    public ResponseEntity<Void> deleteScale(@PathVariable Long id) {
        scaleService.deleteScale(id);
        return ResponseEntity.noContent().build();
    }
}
