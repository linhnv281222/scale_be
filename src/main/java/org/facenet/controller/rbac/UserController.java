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
import org.facenet.dto.rbac.UserDto;
import org.facenet.service.rbac.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs quản lý người dùng")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGE')")
    @Operation(summary = "Lấy danh sách tất cả users với roles và permissions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<UserDto.Response>>> getAllUsers() {
        List<UserDto.Response> users = userService.getAllUsers();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy thông tin chi tiết user với roles và permissions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> getUserById(
            @Parameter(description = "ID của user", example = "1") @PathVariable("id") Long id) {
        UserDto.Response user = userService.getUserById(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo user mới với roles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tạo user thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Username đã tồn tại",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> createUser(
            @Valid @RequestBody UserDto.CreateRequest request) {
        UserDto.Response user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(org.facenet.common.response.ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        UserDto.Response user = userService.updateUser(id, request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user, "User updated successfully"));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật roles cho user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật roles thành công"),
        @ApiResponse(responseCode = "404", description = "User hoặc Role không tồn tại",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> updateUserRoles(
            @Parameter(description = "ID của user", example = "1") @PathVariable("id") Long id,
            @RequestBody Map<String, List<Integer>> request) {
        List<Integer> roleIds = request.get("role_ids");
        UserDto.Response user = userService.updateUserRoles(id, roleIds);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user, "User roles updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa user thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<Void>> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(null, "User deleted successfully"));
    }
}
