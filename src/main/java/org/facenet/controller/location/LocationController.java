package org.facenet.controller.location;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.location.LocationDto;
import org.facenet.service.location.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Location operations
 * Contains exactly 5 APIs: Create, Update, Delete, GetById, GetList
 */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Location Management", description = "APIs for managing locations")
public class LocationController {

    private final LocationService locationService;

    /**
     * 1. Get all locations - Tree structure or Paginated list
     * If tree=true or no params: Return tree structure
     * If search/code/parentId/page/size provided: Return paginated list with filters
     * 
     * Supports:
     * - tree: Return tree structure (default if no other params)
     * - search: Search by name, code, description
     * - code: Filter by exact code
     * - parentId: Filter by parent location
     * - page, size: Pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all locations", 
               description = "Get locations in tree structure (default) or paginated list with filters. Use tree=true for tree, or add search/filter params for paginated list")
    public ResponseEntity<ApiResponse<?>> getAllLocations(
            @RequestParam(value = "tree", required = false) Boolean tree,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        
        // If tree=true OR no params at all, return tree structure
        boolean shouldReturnTree = (tree != null && tree) || 
                                   (page == null && size == null && search == null && code == null && parentId == null);
        
        if (shouldReturnTree) {
            List<LocationDto.Response> locations = locationService.getLocationsTree();
            return ResponseEntity.ok(ApiResponse.success(locations));
        }
        
        // Otherwise, return paginated list with filters
        Map<String, String> filters = new java.util.HashMap<>();
        if (code != null) {
            filters.put("code", code);
        }
        if (parentId != null) {
            filters.put("parent.id", parentId.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page != null ? page : 0)
            .size(size != null ? size : 10)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<LocationDto.Response> locations = locationService.getAllLocations(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    /**
     * 2. Get location by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get location by ID", description = "Retrieve a location by its ID")
    public ResponseEntity<ApiResponse<LocationDto.Response>> getLocationById(
            @PathVariable("id") Long id) {
        LocationDto.Response location = locationService.getLocationById(id);
        return ResponseEntity.ok(ApiResponse.success(location));
    }

    /**
     * 3. Create a new location
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create location", description = "Create a new location")
    public ResponseEntity<ApiResponse<LocationDto.Response>> createLocation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Location data",
                required = true,
                content = @Content(schema = @Schema(implementation = LocationDto.Request.class))
            )
            @Valid @RequestBody LocationDto.Request request) {
        LocationDto.Response location = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(location));
    }

    /**
     * 4. Update location
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update location", description = "Update an existing location")
    public ResponseEntity<ApiResponse<LocationDto.Response>> updateLocation(
            @PathVariable("id") Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated location data",
                required = true,
                content = @Content(schema = @Schema(implementation = LocationDto.Request.class))
            )
            @Valid @RequestBody LocationDto.Request request) {
        LocationDto.Response location = locationService.updateLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success(location));
    }

    /**
     * 5. Delete location (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete location (soft delete)", description = "Soft delete a location (sets is_active to false)")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable("id") Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}