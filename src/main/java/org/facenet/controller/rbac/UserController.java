package org.facenet.controller.rbac;

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
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGE')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<List<UserDto.Response>>> getAllUsers() {
        List<UserDto.Response> users = userService.getAllUsers();
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> getUserById(
            @PathVariable("id") Long id) {
        UserDto.Response user = userService.getUserById(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> createUser(
            @Valid @RequestBody UserDto.CreateRequest request) {
        UserDto.Response user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(org.facenet.common.response.ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        UserDto.Response user = userService.updateUser(id, request);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user, "User updated successfully"));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<UserDto.Response>> updateUserRoles(
            @PathVariable("id") Long id,
            @RequestBody Map<String, List<Integer>> request) {
        List<Integer> roleIds = request.get("role_ids");
        UserDto.Response user = userService.updateUserRoles(id, roleIds);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(user, "User roles updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.facenet.common.response.ApiResponse<Void>> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(org.facenet.common.response.ApiResponse.success(null, "User deleted successfully"));
    }
}
