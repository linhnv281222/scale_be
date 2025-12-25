package org.facenet.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ErrorResponse;
import org.facenet.dto.auth.AuthDto;
import org.facenet.dto.rbac.UserDto;
import org.facenet.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs xác thực và quản lý JWT tokens")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "Đăng nhập hệ thống",
        description = "Xác thực người dùng và trả về JWT access token và refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
        @ApiResponse(responseCode = "400", description = "Thông tin đăng nhập không hợp lệ",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Làm mới access token",
        description = "Sử dụng refresh token để lấy access token mới"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token đã được làm mới thành công"),
        @ApiResponse(responseCode = "400", description = "Refresh token không hợp lệ",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<AuthDto.LoginResponse>> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        AuthDto.LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Đăng xuất",
        description = "Đăng xuất người dùng (client cần xóa token đã lưu)"
    )
    public ResponseEntity<org.facenet.common.response.ApiResponse<Void>> logout() {
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Lấy thông tin người dùng hiện tại",
        description = "Lấy thông tin chi tiết của người dùng đã xác thực"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công"),
        @ApiResponse(responseCode = "401", description = "Người dùng chưa xác thực",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> getCurrentUser() {
        UserDto.Response user = authService.getCurrentUser();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user, "User information retrieved successfully"));
    }
}
