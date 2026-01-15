package org.facenet.service.location;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.location.LocationDto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Location operations
 * Supports 5 core operations: GetList, GetById, Create, Update, Delete
 */
public interface LocationService {

    /**
     * Get all locations with pagination and filters (flat list)
     */
    PageResponseDto<LocationDto.Response> getAllLocations(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get locations in tree structure
     */
    List<LocationDto.Response> getLocationsTree();

    /**
     * Get location tree from specific location (by code or parentId)
     * Returns the matched location as root with all its children
     */
    LocationDto.Response getLocationTreeByCode(String code);

    /**
     * Get location tree from specific location by ID
     * Returns the location as root with all its children
     */
    LocationDto.Response getLocationTreeById(Long id);

    /**
     * Get location by ID
     */
    LocationDto.Response getLocationById(Long id);

    /**
     * Create a new location
     */
    LocationDto.Response createLocation(LocationDto.Request request);

    /**
     * Update location
     */
    LocationDto.Response updateLocation(Long id, LocationDto.Request request);

    /**
     * Delete location
     */
    void deleteLocation(Long id);
}