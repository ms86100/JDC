package com.avionics_systems.migration.config;

import com.avionics_systems.migration.entity.WizardSession;
import com.avionics_systems.migration.repository.WizardSessionRepository;
import com.avionics_systems.migration.security.MigrationJwtValidator;
import com.avionics_systems.migration.security.MigrationProjectAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Migration API security: JWT Bearer and/or headers, role checks, project scope (MG-P0-4).
 */
@Component
@RequiredArgsConstructor
public class MigrationHeaderAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_MIGRATION_ROLE = "migrationRole";
    public static final String ATTR_USER_ID = "migrationUserId";

    @Value("${app.security.write-roles:MIGRATION_ADMIN,MIGRATION_OPERATOR}")
    private String writeRolesStr;

    @Value("${app.security.read-roles:MIGRATION_ADMIN,MIGRATION_OPERATOR,MIGRATION_VIEWER}")
    private String readRolesStr;

    @Value("${app.security.admin-only-paths:/api/migration/dlq/purge,/api/migration/dlq/retry/all}")
    private String adminOnlyPathsStr;

    private Set<String> getWriteRoles() {
        return Set.of(writeRolesStr.split(","));
    }

    private Set<String> getReadRoles() {
        return Set.of(readRolesStr.split(","));
    }

    private Set<String> getAdminOnlyPaths() {
        return Set.of(adminOnlyPathsStr.split(","));
    }

    private final MigrationJwtValidator jwtValidator;
    private final MigrationProjectAccessService projectAccessService;
    private final WizardSessionRepository wizardSessionRepository;

    @Value("${migration.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${migration.security.require-jwt:false}")
    private boolean requireJwt;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/migration");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!securityEnabled) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        var parsedJwt = jwtValidator.parseBearer(authHeader);

        UUID userId;
        if (parsedJwt.isPresent()) {
            userId = parsedJwt.get().userId();
        } else if (requireJwt) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Valid Bearer JWT is required");
            return;
        } else {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader == null || userIdHeader.isBlank()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "X-User-Id or Bearer JWT is required");
                return;
            }
            try {
                userId = UUID.fromString(userIdHeader.trim());
            } catch (IllegalArgumentException e) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid X-User-Id");
                return;
            }
        }

        String headerRole = request.getHeader("X-Migration-Role");
        String migrationRole = parsedJwt
                .map(j -> MigrationJwtValidator.resolveMigrationRole(j.platformRoles(), headerRole))
                .orElse(headerRole != null && !headerRole.isBlank() ? headerRole.trim() : "MIGRATION_OPERATOR");

        String method = request.getMethod();
        boolean write = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
        Set<String> allowed = write ? getWriteRoles() : getReadRoles();

        if (isAdminOnlyPath(request.getRequestURI(), method) && !"MIGRATION_ADMIN".equals(migrationRole)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "MIGRATION_ADMIN role required");
            return;
        }

        if (!allowed.contains(migrationRole)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Insufficient migration role");
            return;
        }

        if (write && requiresProjectScope(request.getRequestURI(), method)) {
            try {
                UUID projectId = extractTargetProjectId(request);
                projectAccessService.requireProjectHeaderForImport(projectId, migrationRole);
            } catch (SecurityException e) {
                response.sendError(HttpStatus.FORBIDDEN.value(), e.getMessage());
                return;
            }
        }

        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_MIGRATION_ROLE, migrationRole);
        chain.doFilter(request, response);
    }

    private boolean isAdminOnlyPath(String uri, String method) {
        if (!"DELETE".equals(method) && !"POST".equals(method)) {
            return false;
        }
        return getAdminOnlyPaths().stream().anyMatch(uri::startsWith);
    }

    private boolean requiresProjectScope(String uri, String method) {
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            return false;
        }
        if (uri.contains("/validate")) {
            return false;
        }
        if (uri.startsWith("/api/migration/import/csv")
                || uri.startsWith("/api/migration/import/legacy-dc")
                || uri.startsWith("/api/migration/import/project")) {
            return !uri.contains("/validate");
        }
        if (uri.contains("/wizard/sessions") && uri.contains("/execute")) {
            return true;
        }
        return false;
    }

    private UUID extractTargetProjectId(HttpServletRequest request) {
        String header = request.getHeader("X-Target-Project-Id");
        if (header != null && !header.isBlank()) {
            return UUID.fromString(header.trim());
        }
        String param = request.getParameter("targetProjectId");
        if (param != null && !param.isBlank()) {
            return UUID.fromString(param.trim());
        }
        return resolveTargetProjectFromWizardSession(request.getRequestURI());
    }

    /** Wizard execute sends targetProjectId in JSON body; fall back to persisted session. */
    private UUID resolveTargetProjectFromWizardSession(String uri) {
        if (uri == null || !uri.contains("/wizard/sessions/") || !uri.endsWith("/execute")) {
            return null;
        }
        String[] parts = uri.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("sessions".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    UUID sessionId = UUID.fromString(parts[i + 1]);
                    return wizardSessionRepository.findById(sessionId)
                            .map(WizardSession::getTargetProjectId)
                            .orElse(null);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
