package org.facenet.controller.rbac;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.rbac.PermissionDto;
import org.facenet.service.rbac.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<PermissionDto.Response>>> getAllPermissions() {
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
