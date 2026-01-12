package org.facenet.controller.rbac;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.rbac.RoleDto;
import org.facenet.service.rbac.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * Get all roles with pagination and filters
     * Supports filters: isActive, code, name
     * Examples:
     * - /roles?page=0&size=10
     * - /roles?isActive=true&page=0&size=10
     * - /roles?search=admin&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<?>> getAllRoles(
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
        
        PageResponseDto<RoleDto.Response> roles = roleService.getAllRoles(pageRequest, filters);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(roles));
    }

    /**
     * Get all roles without pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<RoleDto.Response>>> getAllRolesList() {
        List<RoleDto.Response> roles = roleService.getAllRoles();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<RoleDto.Response>> getRoleById(
            @PathVariable Integer id) {
        RoleDto.Response role = roleService.getRoleById(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(role));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<RoleDto.Response>> createRole(
            @Valid @RequestBody RoleDto.Request request) {
        RoleDto.Response role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(org.facenet.common.response.ApiResponse.success(role, "Role created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<RoleDto.Response>> updateRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleDto.Request request) {
        RoleDto.Response role = roleService.updateRole(id, request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(role, "Role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<Void>> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(null, "Role deleted successfully"));
    }
}
