package com.jira.migration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the Jira Migration Service.
 * Provides role-based access control with proper CSRF protection.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                // Use Cookie-based CSRF token for SPA clients
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Use custom handler to read token from header
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Disable CSRF only for API endpoints that use custom tokens
                // For production, consider using JWT-based authentication instead
                .ignoringRequestMatchers(
                    "/actuator/**",           // Health checks
                    "/ws/**",                 // WebSocket endpoints
                    "/api/auth/**",           // Auth endpoints
                    "/api/migration/**",      // Migration endpoints (uses custom auth)
                    "/swagger-ui/**",         // API docs
                    "/v3/api-docs/**"        // OpenAPI spec
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - health checks, actuator, docs
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/ws/**").permitAll()

                // Field discovery is read-only, allow for authenticated users
                .requestMatchers(HttpMethod.POST, "/api/fields/discover").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/fields/discover/bulk").authenticated()

                // Field mapping is read-only
                .requestMatchers(HttpMethod.POST, "/api/fields/map").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/fields/map/suggestions").authenticated()

                // Screen configuration and search are read-only
                .requestMatchers(HttpMethod.GET, "/api/fields/screens/configuration").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/fields/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/fields/by-type/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/fields/by-region/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/fields/plugin/**").permitAll()

                // Field statistics
                .requestMatchers(HttpMethod.GET, "/api/fields/statistics/**").authenticated()

                // Field value operations require authentication
                .requestMatchers(HttpMethod.GET, "/api/fields/issues/*/values").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/fields/issues/*/values").hasRole("USER")

                // Field definition management requires ADMIN
                .requestMatchers(HttpMethod.GET, "/api/fields").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/fields/definitions").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/fields/definitions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/fields/definitions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/fields/definitions/**").hasRole("ADMIN")

                // Custom field operations require ADMIN
                .requestMatchers(HttpMethod.POST, "/api/fields/custom").hasRole("ADMIN")

                // Field provisioning requires ADMIN
                .requestMatchers(HttpMethod.POST, "/api/fields/provision").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/fields/provision/initialize").hasRole("ADMIN")

                // Migration-specific field operations
                .requestMatchers(HttpMethod.POST, "/api/fields/migration/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "https://*.atlassian.net",
            "https://*.jira.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "X-CSRF-Token",
            "X-User-Id",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of(
            "X-CSRF-Token",
            "X-RateLimit-Remaining",
            "X-RateLimit-Limit",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}