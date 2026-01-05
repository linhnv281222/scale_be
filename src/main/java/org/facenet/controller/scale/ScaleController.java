package org.facenet.controller.scale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
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
import java.util.Map;

@RestController
@RequestMapping("/scales")
@RequiredArgsConstructor
public class ScaleController {

    private final ScaleService scaleService;
    private final WeighingLogService weighingLogService;

    /**
     * Get all scales with pagination and filters
     * Supports filters: locationId, manufacturerId, direction, isActive, model, name
     * Examples:
     * - /scales?page=0&size=10
     * - /scales?locationId=1&isActive=true
     * - /scales?manufacturerId=2&direction=IMPORT
     * - /scales?search=warehouse&sort=name,asc
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all scales", description = "Retrieve all scales with pagination and filters. Supports: locationId, manufacturerId, direction (IMPORT/EXPORT), isActive, model, name")
    public ResponseEntity<ApiResponse<PageResponseDto<ScaleDto.Response>>> getAllScales(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "locationId", required = false) Long locationId,
            @RequestParam(value = "manufacturerId", required = false) Long manufacturerId,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(required = false) Map<String, String> allParams) {
        
        // Build filters map from specific and generic params
        Map<String, String> filters = new java.util.HashMap<>(allParams);
        
        // Remove pagination params
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("search");
        
        // Add specific filters if provided (overwrites any from allParams)
        if (locationId != null) {
            filters.put("location.id", locationId.toString());
        }
        if (manufacturerId != null) {
            filters.put("manufacturer.id", manufacturerId.toString());
        }
        if (direction != null) {
            filters.put("direction", direction);
        }
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<ScaleDto.Response> scales = scaleService.getAllScales(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(scales));
    }

    /**
     * Get all scales without pagination (for dropdown/select)
     * Supports same filters as main endpoint
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all scales", description = "Retrieve all scales without pagination. Supports filters: locationId, manufacturerId, direction, isActive")
    public ResponseEntity<ApiResponse<List<ScaleDto.Response>>> getAllScalesList(
            @RequestParam(value = "locationId", required = false) Long locationId,
            @RequestParam(value = "manufacturerId", required = false) Long manufacturerId,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        
        // If no filters, return all
        if (locationId == null && manufacturerId == null && direction == null && isActive == null) {
            return ResponseEntity.ok(ApiResponse.success(scaleService.getAllScales()));
        }
        
        // Build filters and use paginated endpoint without limit
        Map<String, String> filters = new java.util.HashMap<>();
        if (locationId != null) {
            filters.put("location.id", locationId.toString());
        }
        if (manufacturerId != null) {
            filters.put("manufacturer.id", manufacturerId.toString());
        }
        if (direction != null) {
            filters.put("direction", direction);
        }
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
        }
        
        // Get with high page size to simulate "all"
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(0)
            .size(10000)
            .build();
        
        PageResponseDto<ScaleDto.Response> result = scaleService.getAllScales(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(result.getContent()));
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