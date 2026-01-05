package org.facenet.controller.location;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ApiResponse;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.location.LocationDto;
import org.facenet.service.location.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor

public class LocationController {

    private final LocationService locationService;

    /**
     * Get all locations with pagination and filters
     * Supports filters: parentId, code, name, address
     * Examples:
     * - /locations?parentId=1&page=0&size=10
     * - /locations?name_like=warehouse&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PageResponseDto<LocationDto.Response>>> getAllLocations(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam(required = false) Map<String, String> allParams) {
        
        Map<String, String> filters = new java.util.HashMap<>(allParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("search");
        
        if (parentId != null) {
            filters.put("parent.id", parentId.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<LocationDto.Response> locations = locationService.getAllLocations(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    /**
     * Get all locations without pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<LocationDto.Response>>> getAllLocationsList(
            @RequestParam(value = "parentId", required = false) Long parentId) {
        
        if (parentId == null) {
            return ResponseEntity.ok(ApiResponse.success(locationService.getAllLocations()));
        }
        
        Map<String, String> filters = new java.util.HashMap<>();
        filters.put("parent.id", parentId.toString());
        
        PageRequestDto pageRequest = PageRequestDto.builder().page(0).size(10000).build();
        PageResponseDto<LocationDto.Response> result = locationService.getAllLocations(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(result.getContent()));
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