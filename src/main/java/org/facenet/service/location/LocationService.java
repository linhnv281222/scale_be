package org.facenet.service.location;

import org.facenet.dto.location.LocationDto;

import java.util.List;

/**
 * Service interface for Location operations
 */
public interface LocationService {

    /**
     * Get all locations (flat list)
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