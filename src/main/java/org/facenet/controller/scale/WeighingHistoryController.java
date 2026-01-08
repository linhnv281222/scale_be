package org.facenet.controller.scale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.scale.WeighingDataDto;
import org.facenet.service.scale.WeighingLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Weighing History (Lịch sử đọc dữ liệu cân)
 */
@RestController
@RequestMapping("/weighing-history")
@RequiredArgsConstructor
@Tag(name = "Weighing History", description = "APIs for querying weighing data history from scales")
public class WeighingHistoryController {

    private final WeighingLogService weighingLogService;

    /**
     * Get weighing history with advanced filters
     * 
     * Supports:
     * - Pagination: page, size, sort
     * - Search: Global search on scale name and model
     * - Filters:
     *   + scaleId: Filter by scale ID
     *   + scaleCode: Filter by scale code (name)
     *   + direction: Filter by scale direction (IMPORT/EXPORT)
     *   + locationId: Filter by location
     *   + protocolId: Filter by protocol
     *   + startTime: Filter records from this time
     *   + endTime: Filter records until this time
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(
        summary = "Get weighing history",
        description = "Retrieve paginated weighing history with filters. " +
                     "Search: scale name, model. " +
                     "Filters: scaleId, scaleCode, direction (IMPORT/EXPORT), locationId, protocolId, startTime, endTime. " +
                     "Default sort: createdAt DESC"
    )
    public ResponseEntity<PageResponseDto<WeighingDataDto.HistoryResponse>> getWeighingHistory(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            
            @Parameter(description = "Page size")
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            
            @Parameter(description = "Sort field and direction (e.g., 'createdAt,desc')")
            @RequestParam(value = "sort", required = false) String sort,
            
            @Parameter(description = "Global search keyword for scale name or model")
            @RequestParam(value = "search", required = false) String search,
            
            @Parameter(description = "Filter by scale ID")
            @RequestParam(value = "scaleId", required = false) Long scaleId,
            
            @Parameter(description = "Filter by scale code/name (partial match)")
            @RequestParam(value = "scaleCode", required = false) String scaleCode,
            
            @Parameter(description = "Filter by scale direction (IMPORT or EXPORT)")
            @RequestParam(value = "direction", required = false) String direction,
            
            @Parameter(description = "Filter by location ID")
            @RequestParam(value = "locationId", required = false) Long locationId,
            
            @Parameter(description = "Filter by protocol ID")
            @RequestParam(value = "protocolId", required = false) Long protocolId,
            
            @Parameter(description = "Filter records from this time (ISO 8601 format with timezone)")
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            
            @Parameter(description = "Filter records until this time (ISO 8601 format with timezone)")
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        
        // Build filters map
        Map<String, String> filters = new HashMap<>();
        
        if (scaleId != null) {
            filters.put("scaleId", String.valueOf(scaleId));
        }
        if (scaleCode != null && !scaleCode.isBlank()) {
            filters.put("scaleCode", scaleCode);
        }
        if (direction != null && !direction.isBlank()) {
            filters.put("direction", direction);
        }
        if (locationId != null) {
            filters.put("locationId", String.valueOf(locationId));
        }
        if (protocolId != null) {
            filters.put("protocolId", String.valueOf(protocolId));
        }
        if (startTime != null) {
            filters.put("startTime", startTime.toString());
        }
        if (endTime != null) {
            filters.put("endTime", endTime.toString());
        }
        
        // Build page request
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        // Execute query
        PageResponseDto<WeighingDataDto.HistoryResponse> response = 
            weighingLogService.getWeighingHistory(pageRequest, filters);
        
        return ResponseEntity.ok(response);
    }
}
