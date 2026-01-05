package org.facenet.service.location;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.location.LocationDto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Location operations
 */
public interface LocationService {

    /**
     * Get all locations with pagination and filters
     */
    PageResponseDto<LocationDto.Response> getAllLocations(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all locations (flat list, non-paginated)
     */
    List<LocationDto.Response> getAllLocations();

    /**
     * Get locations in tree structure
     */
    List<LocationDto.Response> getLocationsTree();

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