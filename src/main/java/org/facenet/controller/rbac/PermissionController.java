package org.facenet.controller.rbac;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.rbac.PermissionDto;
import org.facenet.service.rbac.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * Get all permissions with pagination and filters
     * Supports filters: isActive, code
     * Examples:
     * - /permissions?page=0&size=10
     * - /permissions?isActive=true&page=0&size=10
     * - /permissions?search=read&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<?>> getAllPermissions(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(required = false) Map<String, String> allParams) {
        
        Map<String, String> filters = new java.util.HashMap<>(allParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("search");
        
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<PermissionDto.Response> permissions = permissionService.getAllPermissions(pageRequest, filters);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(permissions));
    }

    /**
     * Get all permissions without pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<PermissionDto.Response>>> getAllPermissionsList() {
        List<PermissionDto.Response> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(permissions));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<PermissionDto.Response>> getPermissionById(
            @PathVariable("id") Integer id) {
        PermissionDto.Response permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(permission));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<PermissionDto.Response>> createPermission(
            @Valid @RequestBody PermissionDto.Request request) {
        PermissionDto.Response permission = permissionService.createPermission(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(org.facenet.common.response.ApiResponse.success(permission, "Permission created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<PermissionDto.Response>> updatePermission(
            @PathVariable("id") Integer id,
            @Valid @RequestBody PermissionDto.Request request) {
        PermissionDto.Response permission = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(permission, "Permission updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePermission(
            @PathVariable("id") Integer id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
