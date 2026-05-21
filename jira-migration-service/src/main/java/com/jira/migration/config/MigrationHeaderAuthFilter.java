package com.jira.migration.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Lightweight RBAC for migration APIs: requires X-User-Id and optional X-Migration-Role (P1-08 / P9-02).
 */
@Component
public class MigrationHeaderAuthFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_ROLES = Set.of("MIGRATION_ADMIN", "MIGRATION_OPERATOR");
    private static final Set<String> READ_ROLES = Set.of("MIGRATION_ADMIN", "MIGRATION_OPERATOR", "MIGRATION_VIEWER");

    @Value("${migration.security.enabled:true}")
    private boolean securityEnabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/migration");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!securityEnabled) {
            chain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "X-User-Id header is required");
            return;
        }
        try {
            UUID.fromString(userIdHeader.trim());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid X-User-Id");
            return;
        }

        String role = request.getHeader("X-Migration-Role");
        if (role == null || role.isBlank()) {
            role = "MIGRATION_OPERATOR";
        }

        String method = request.getMethod();
        boolean write = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
        Set<String> allowed = write ? WRITE_ROLES : READ_ROLES;
        if (!allowed.contains(role)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Insufficient migration role");
            return;
        }

        chain.doFilter(request, response);
    }
}
