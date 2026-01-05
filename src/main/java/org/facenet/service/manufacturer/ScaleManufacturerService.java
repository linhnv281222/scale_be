package org.facenet.service.manufacturer;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.manufacturer.ScaleManufacturerDto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for ScaleManufacturer operations
 */
public interface ScaleManufacturerService {

    /**
     * Get all manufacturers with pagination and filters
     */
    PageResponseDto<ScaleManufacturerDto.Response> getAllManufacturers(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all manufacturers (non-paginated)
     */
    List<ScaleManufacturerDto.Response> getAllManufacturers();

    /**
     * Get all active manufacturers
     */
    List<ScaleManufacturerDto.Response> getAllActiveManufacturers();

    /**
     * Get manufacturer by ID
     */
    ScaleManufacturerDto.Response getManufacturerById(Long id);

    /**
     * Search manufacturers by name or code
     */
    List<ScaleManufacturerDto.Response> searchManufacturers(String search);

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

    /**
     * Toggle manufacturer active status
     */
    ScaleManufacturerDto.Response toggleActiveStatus(Long id);
}
