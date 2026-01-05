package org.facenet.controller.manufacturer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.manufacturer.ScaleManufacturerDto;
import org.facenet.service.manufacturer.ScaleManufacturerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for ScaleManufacturer operations
 */
@RestController
@RequestMapping("/manufacturers")
@RequiredArgsConstructor
public class ScaleManufacturerController {

    private final ScaleManufacturerService manufacturerService;

    /**
     * Get all manufacturers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<ScaleManufacturerDto.Response>>> getAllManufacturers() {
        List<ScaleManufacturerDto.Response> manufacturers = manufacturerService.getAllManufacturers();
        return ResponseEntity.ok(ApiResponse.success(manufacturers));
    }

    /**
     * Get all active manufacturers
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<ScaleManufacturerDto.Response>>> getAllActiveManufacturers() {
        List<ScaleManufacturerDto.Response> manufacturers = manufacturerService.getAllActiveManufacturers();
        return ResponseEntity.ok(ApiResponse.success(manufacturers));
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
     * Search manufacturers by name or code
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<ScaleManufacturerDto.Response>>> searchManufacturers(
            @RequestParam("q") String search) {
        List<ScaleManufacturerDto.Response> manufacturers = manufacturerService.searchManufacturers(search);
        return ResponseEntity.ok(ApiResponse.success(manufacturers));
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
