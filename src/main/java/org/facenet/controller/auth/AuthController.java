package org.facenet.controller.auth;

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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<org.facenet.common.response.ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<org.facenet.common.response.ApiResponse<AuthDto.LoginResponse>> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        AuthDto.LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<org.facenet.common.response.ApiResponse<Void>> logout() {
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> getCurrentUser() {
        UserDto.Response user = authService.getCurrentUser();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user, "User information retrieved successfully"));
    }
}
