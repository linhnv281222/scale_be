package org.facenet.controller.rbac;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Role Management", description = "APIs quản lý vai trò")
@SecurityRequirement(name = "Bearer Authentication")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy danh sách tất cả roles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<RoleDto.Response>>> getAllRoles() {
        List<RoleDto.Response> roles = roleService.getAllRoles();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy thông tin chi tiết role với permissions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy role",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<RoleDto.Response>> getRoleById(
            @Parameter(description = "ID của role", example = "1") @PathVariable Integer id) {
        RoleDto.Response role = roleService.getRoleById(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(role));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo role mới và gán permissions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tạo role thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Role code đã tồn tại",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<RoleDto.Response>> createRole(
            @Valid @RequestBody RoleDto.Request request) {
        RoleDto.Response role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(org.facenet.common.response.ApiResponse.success(role, "Role created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy role",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<RoleDto.Response>> updateRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleDto.Request request) {
        RoleDto.Response role = roleService.updateRole(id, request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(role, "Role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa role thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy role",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<Void>> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(null, "Role deleted successfully"));
    }
}
