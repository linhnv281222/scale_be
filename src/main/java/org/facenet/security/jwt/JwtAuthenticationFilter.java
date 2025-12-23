package org.facenet.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT authentication filter to validate tokens on each request
 * Extracts username, roles, and permissions directly from JWT token
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                
                // Extract authorities from token instead of database lookup
                List<GrantedAuthority> authorities = getAuthoritiesFromToken(jwt);
                
                // Create UserDetails with authorities from token
                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("") // Password not needed for JWT auth
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract authorities (roles and permissions) from JWT token
     */
    private List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Add roles
        List<String> roles = tokenProvider.getRolesFromToken(token);
        if (roles != null) {
            roles.forEach(role -> 
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }
        
        // Add permissions
        List<String> permissions = tokenProvider.getPermissionsFromToken(token);
        if (permissions != null) {
            permissions.forEach(permission -> 
                authorities.add(new SimpleGrantedAuthority(permission))
            );
        }
        
        return authorities;
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
