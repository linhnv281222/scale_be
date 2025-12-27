package org.facenet.controller.location;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.location.LocationDto;
import org.facenet.service.location.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor

public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<LocationDto.Response>>> getAllLocations() {
        List<LocationDto.Response> locations = locationService.getAllLocations();
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<LocationDto.Response>>> getLocationsTree() {
        List<LocationDto.Response> locations = locationService.getLocationsTree();
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<LocationDto.Response>> getLocationById(
            @PathVariable("id") Long id) {
        LocationDto.Response location = locationService.getLocationById(id);
        return ResponseEntity.ok(ApiResponse.success(location));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LocationDto.Response>> createLocation(
            @Valid @RequestBody LocationDto.Request request) {
        LocationDto.Response location = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(location));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LocationDto.Response>> updateLocation(
            @PathVariable("id") Long id,
            @Valid @RequestBody LocationDto.Request request) {
        LocationDto.Response location = locationService.updateLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success(location));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable("id") Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}