package org.facenet.controller.rbac;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.rbac.RoleDto;
import org.facenet.service.rbac.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<RoleDto.Response>>> getAllRoles() {
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
