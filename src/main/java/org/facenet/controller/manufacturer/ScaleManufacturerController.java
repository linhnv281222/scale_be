package org.facenet.controller.manufacturer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.manufacturer.ScaleManufacturerDto;
import org.facenet.service.manufacturer.ScaleManufacturerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for ScaleManufacturer operations
 */
@RestController
@RequestMapping("/manufacturers")
@RequiredArgsConstructor
public class ScaleManufacturerController {

    private final ScaleManufacturerService manufacturerService;

    /**
     * Get all manufacturers with pagination and filters
     * Supports filters: country, isActive, code, name
     * Examples:
     * - /manufacturers?country=Vietnam&isActive=true
     * - /manufacturers?name_like=toledo&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PageResponseDto<ScaleManufacturerDto.Response>>> getAllManufacturers(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(required = false) Map<String, String> allParams) {
        
        Map<String, String> filters = new java.util.HashMap<>(allParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("search");
        
        if (country != null) {
            filters.put("country", country);
        }
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<ScaleManufacturerDto.Response> manufacturers = manufacturerService.getAllManufacturers(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(manufacturers));
    }

    /**
     * Get all manufacturers without pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<ScaleManufacturerDto.Response>>> getAllManufacturersList(
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        
        if (country == null && isActive == null) {
            return ResponseEntity.ok(ApiResponse.success(manufacturerService.getAllManufacturers()));
        }
        
        Map<String, String> filters = new java.util.HashMap<>();
        if (country != null) {
            filters.put("country", country);
        }
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder().page(0).size(10000).build();
        PageResponseDto<ScaleManufacturerDto.Response> result = manufacturerService.getAllManufacturers(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(result.getContent()));
    }

    /**
     * Get manufacturer by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ScaleManufacturerDto.Response>> getManufacturerById(
            @PathVariable("id") Long id) {
        ScaleManufacturerDto.Response manufacturer = manufacturerService.getManufacturerById(id);
        return ResponseEntity.ok(ApiResponse.success(manufacturer));
    }

    /**

     * Create a new manufacturer
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ScaleManufacturerDto.Response>> createManufacturer(
            @Valid @RequestBody ScaleManufacturerDto.Request request) {
        ScaleManufacturerDto.Response manufacturer = manufacturerService.createManufacturer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(manufacturer));
    }

    /**
     * Update manufacturer
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ScaleManufacturerDto.Response>> updateManufacturer(
            @PathVariable("id") Long id,
            @Valid @RequestBody ScaleManufacturerDto.Request request) {
        ScaleManufacturerDto.Response manufacturer = manufacturerService.updateManufacturer(id, request);
        return ResponseEntity.ok(ApiResponse.success(manufacturer));
    }

    /**
     * Delete manufacturer
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteManufacturer(
            @PathVariable("id") Long id) {
        manufacturerService.deleteManufacturer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle manufacturer active status
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ScaleManufacturerDto.Response>> toggleActiveStatus(
            @PathVariable("id") Long id) {
        ScaleManufacturerDto.Response manufacturer = manufacturerService.toggleActiveStatus(id);
        return ResponseEntity.ok(ApiResponse.success(manufacturer));
    }
}
