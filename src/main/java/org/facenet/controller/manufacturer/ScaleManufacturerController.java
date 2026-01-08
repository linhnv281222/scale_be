package org.facenet.controller.manufacturer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.Map;

/**
 * Controller for ScaleManufacturer operations
 * Contains exactly 5 APIs: Create, Update, Delete, GetById, GetList
 */
@RestController
@RequestMapping("/manufacturers")
@RequiredArgsConstructor
@Tag(name = "Scale Manufacturer Management", description = "APIs for managing scale manufacturers/brands")
public class ScaleManufacturerController {

    private final ScaleManufacturerService manufacturerService;

    /**
     * 1. Get all manufacturers with pagination and filters
     * Supports:
     * - search: Search by name, code, country, description
     * - code: Filter by exact code
     * - country: Filter by country
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all manufacturers", 
               description = "Get list of manufacturers with pagination. Supports search (name/code/country/description), filter by code and country")
    public ResponseEntity<ApiResponse<PageResponseDto<ScaleManufacturerDto.Response>>> getAllManufacturers(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "country", required = false) String country) {
        
        Map<String, String> filters = new java.util.HashMap<>();
        if (code != null) {
            filters.put("code", code);
        }
        if (country != null) {
            filters.put("country", country);
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
     * 2. Get manufacturer by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get manufacturer by ID", description = "Retrieve a manufacturer by its ID")
    public ResponseEntity<ApiResponse<ScaleManufacturerDto.Response>> getManufacturerById(
            @PathVariable("id") Long id) {
        ScaleManufacturerDto.Response manufacturer = manufacturerService.getManufacturerById(id);
        return ResponseEntity.ok(ApiResponse.success(manufacturer));
    }

    /**
     * 3. Create a new manufacturer
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create manufacturer", description = "Create a new scale manufacturer")
    public ResponseEntity<ApiResponse<ScaleManufacturerDto.Response>> createManufacturer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Manufacturer data",
                required = true,
                content = @Content(schema = @Schema(implementation = ScaleManufacturerDto.Request.class))
            )
            @Valid @RequestBody ScaleManufacturerDto.Request request) {
        ScaleManufacturerDto.Response manufacturer = manufacturerService.createManufacturer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(manufacturer));
    }

    /**
     * 4. Update manufacturer
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update manufacturer", description = "Update an existing scale manufacturer")
    public ResponseEntity<ApiResponse<ScaleManufacturerDto.Response>> updateManufacturer(
            @PathVariable("id") Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated manufacturer data",
                required = true,
                content = @Content(schema = @Schema(implementation = ScaleManufacturerDto.Request.class))
            )
            @Valid @RequestBody ScaleManufacturerDto.Request request) {
        ScaleManufacturerDto.Response manufacturer = manufacturerService.updateManufacturer(id, request);
        return ResponseEntity.ok(ApiResponse.success(manufacturer));
    }

    /**
     * 5. Delete manufacturer
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete manufacturer", description = "Delete a scale manufacturer")
    public ResponseEntity<Void> deleteManufacturer(
            @PathVariable("id") Long id) {
        manufacturerService.deleteManufacturer(id);
        return ResponseEntity.noContent().build();
    }
}
