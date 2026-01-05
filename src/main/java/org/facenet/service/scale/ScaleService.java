package org.facenet.service.scale;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.scale.ScaleConfigDto;
import org.facenet.dto.scale.ScaleDto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Scale operations
 */
public interface ScaleService {

    /**
     * Get all scales with pagination and filters
     */
    PageResponseDto<ScaleDto.Response> getAllScales(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all scales (non-paginated)
     */
    List<ScaleDto.Response> getAllScales();

    /**
     * Get scales by location
     */
    List<ScaleDto.Response> getScalesByLocation(Long locationId);

    /**
     * Get scales by multiple locations
     */
    List<ScaleDto.Response> getScalesByLocations(List<Long> locationIds);

    /**
     * Get scales by manufacturer
     */
    List<ScaleDto.Response> getScalesByManufacturer(Long manufacturerId);

    /**
     * Get scales by direction (IMPORT/EXPORT)
     */
    List<ScaleDto.Response> getScalesByDirection(String direction);

    /**
     * Get active scales
     */
    List<ScaleDto.Response> getActiveScales();

    /**
     * Get scale by ID
     */
    ScaleDto.Response getScaleById(Long id);

    /**
     * Create a new scale
     */
    ScaleDto.Response createScale(ScaleDto.Request request);

    /**
     * Update scale
     */
    ScaleDto.Response updateScale(Long id, ScaleDto.Request request);

    /**
     * Delete scale
     */
    void deleteScale(Long id);

    /**
     * Get scale configuration
     */
    ScaleConfigDto.Response getScaleConfig(Long scaleId);

    /**
     * Update scale configuration
     */
    ScaleConfigDto.Response updateScaleConfig(Long scaleId, ScaleConfigDto.Request request);
}