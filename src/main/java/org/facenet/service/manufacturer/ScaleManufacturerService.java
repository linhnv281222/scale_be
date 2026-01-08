package org.facenet.service.manufacturer;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.manufacturer.ScaleManufacturerDto;
import org.springframework.data.repository.query.Param;

import java.util.Map;

/**
 * Service interface for ScaleManufacturer operations
 * Supports 5 core operations: GetList, GetById, Create, Update, Delete
 */
public interface ScaleManufacturerService {

    /**
     * Get all manufacturers with pagination and filters
     * Supports:
     * - search: Search by name, code, country, description
     * - code: Filter by exact code
     * - country: Filter by country
     */
    PageResponseDto<ScaleManufacturerDto.Response> getAllManufacturers(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get manufacturer by ID
     */
    ScaleManufacturerDto.Response getManufacturerById(@Param("id") Long id);

    /**
     * Create a new manufacturer
     */
    ScaleManufacturerDto.Response createManufacturer(ScaleManufacturerDto.Request request);

    /**
     * Update manufacturer
     */
    ScaleManufacturerDto.Response updateManufacturer(Long id, ScaleManufacturerDto.Request request);

    /**
     * Delete manufacturer
     */
    void deleteManufacturer(Long id);
}
