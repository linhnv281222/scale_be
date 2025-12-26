package org.facenet.config;

import lombok.RequiredArgsConstructor;
import org.facenet.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the application
 * JWT-based authentication (Stateless)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // =======================
    // Password encoder
    // =======================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =======================
    // Authentication manager
    // =======================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // =======================
    // CORS configuration
    // =======================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow localhost origins for development
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://localhost:5173",
                "http://localhost:8080",
                "http://103.82.27.132:*",
                "http://103.82.27.132:*",
                "http://103.82.27.132:5173",
                "http://103.82.27.132:8080"

        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // Enable credentials for specific origins
        config.setMaxAge(3600L); // Cache preflight for 1 hour

        // Allow WebSocket handshake headers
        config.setExposedHeaders(List.of(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Authorization",
                "Sec-WebSocket-Accept",
                "Sec-WebSocket-Protocol",
                "Sec-WebSocket-Version"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // =======================
    // Security filter chain
    // =======================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (JWT + Stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS
                .cors(cors -> {})

                // Allow iframe (H2 Console, Swagger UI)
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin())
                )

                // Stateless session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints (allow both with and without /api/v1 prefix)
                        .requestMatchers("/auth/**", "/api/v1/auth/**").permitAll()

                        // WebSocket (cho phép cả endpoint và SockJS paths)
                        .requestMatchers("/ws/**", "/ws-scalehub/**", "/api/v1/ws-scalehub/**").permitAll()
                        
                        // Test endpoints (development only)
                        .requestMatchers("/test/**", "/api/v1/test/**").permitAll()

                        // H2 Console
                        .requestMatchers("/h2-console/**").permitAll()

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
