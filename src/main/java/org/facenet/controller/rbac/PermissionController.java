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
@Tag(name = "Permission Management", description = "APIs quản lý quyền hạn")
@SecurityRequirement(name = "Bearer Authentication")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy danh sách tất cả permissions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<PermissionDto.Response>>> getAllPermissions() {
        List<PermissionDto.Response> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(permissions));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy thông tin chi tiết permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy permission",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<PermissionDto.Response>> getPermissionById(
            @Parameter(description = "ID của permission", example = "1") @PathVariable("id") Integer id) {
        PermissionDto.Response permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(permission));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tạo permission thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Permission code đã tồn tại",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<PermissionDto.Response>> createPermission(
            @Valid @RequestBody PermissionDto.Request request) {
        PermissionDto.Response permission = permissionService.createPermission(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(org.facenet.common.response.ApiResponse.success(permission, "Permission created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy permission",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Permission code đã tồn tại",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<PermissionDto.Response>> updatePermission(
            @Parameter(description = "ID của permission", example = "1") @PathVariable("id") Integer id,
            @Valid @RequestBody PermissionDto.Request request) {
        PermissionDto.Response permission = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(permission, "Permission updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Xóa thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy permission",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Không thể xóa permission đang được sử dụng",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deletePermission(
            @Parameter(description = "ID của permission", example = "1") @PathVariable("id") Integer id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
