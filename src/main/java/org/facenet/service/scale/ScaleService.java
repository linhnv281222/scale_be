package org.facenet.service.scale;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.scale.ScaleDto;
import org.facenet.dto.scale.ScaleStatsResponseDto;
import org.springframework.data.repository.query.Param;

import java.util.Map;

/**
 * Service interface for Scale operations
 */
public interface ScaleService {

    /**
     * Get all scales with pagination and filters
     * Supports filters: name, locationId, manufacturerId, protocolId, direction, isActive
     * Supports search: search on name, model fields
     */
    PageResponseDto<ScaleDto.Response> getAllScales(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all scales with pagination, filters and statistics (V2)
     * Supports filters: name, locationId, manufacturerId, protocolId, direction, isActive
     * Supports search: search on name, model fields
     * Returns: paginated data with total_scales, active_scales, inactive_scales
     */
    ScaleStatsResponseDto getAllScalesV2(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get scale by ID
     */
    ScaleDto.Response getScaleById(@Param("id") Long id);

    /**
     * Create a new scale with configuration
     */
    ScaleDto.Response createScale(ScaleDto.Request request);

    /**
     * Update scale with configuration
     */
    ScaleDto.Response updateScale(@Param("id") Long id, ScaleDto.Request request);

    /**
     * Delete scale
     */
    void deleteScale(@Param("id") Long id);
}