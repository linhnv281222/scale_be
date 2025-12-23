package org.facenet.service.auth;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.ValidationException;
import org.facenet.dto.auth.AuthDto;
import org.facenet.entity.rbac.User;
import org.facenet.repository.rbac.UserRepository;
import org.facenet.security.jwt.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for authentication operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    /**
     * Authenticate user and generate tokens
     */
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens with authorities included
            String accessToken = tokenProvider.generateToken(
                request.getUsername(), 
                authentication.getAuthorities()
            );
            String refreshToken = tokenProvider.generateRefreshToken(
                request.getUsername(),
                authentication.getAuthorities()
            );

            // Get user details
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ValidationException("User not found"));

            return AuthDto.LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .build();

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthDto.LoginResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new ValidationException("Invalid or expired refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException("User not found"));

        // Extract authorities from refresh token
        List<String> authorities = new ArrayList<>();
        
        // Add roles from token
        List<String> roles = tokenProvider.getRolesFromToken(refreshToken);
        if (roles != null) {
            roles.forEach(role -> authorities.add("ROLE_" + role));
        }
        
        // Add permissions from token
        List<String> permissions = tokenProvider.getPermissionsFromToken(refreshToken);
        if (permissions != null) {
            authorities.addAll(permissions);
        }

        // Generate new access token with authorities from refresh token
        String newAccessToken = tokenProvider.generateToken(username, 
            authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList())
        );

        return AuthDto.LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return the same refresh token
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .build();
    }
}
