package com.jira.migration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Validates platform JWT (same secret as jira-auth-service / gateway).
 */
@Component
public class MigrationJwtValidator {

    private final SecretKey secretKey;

    public MigrationJwtValidator(@Value("${jwt.secret:jira-platform-super-secret-key-that-is-at-least-256-bits-long}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public record ParsedJwt(UUID userId, String username, Set<String> platformRoles) {
    }

    public Optional<ParsedJwt> parseBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            Set<String> platformRoles = roles != null ? new HashSet<>(roles) : Set.of();
            return Optional.of(new ParsedJwt(userId, username, platformRoles));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Maps platform roles to migration role for authorization. */
    public static String resolveMigrationRole(Set<String> platformRoles, String headerMigrationRole) {
        if (platformRoles != null) {
            if (platformRoles.contains("ADMIN") || platformRoles.contains("MIGRATION_ADMIN")) {
                return "MIGRATION_ADMIN";
            }
            if (platformRoles.contains("PROJECT_ADMIN") || platformRoles.contains("MIGRATION_OPERATOR")) {
                return "MIGRATION_OPERATOR";
            }
            if (platformRoles.contains("MIGRATION_VIEWER")) {
                return "MIGRATION_VIEWER";
            }
        }
        if (headerMigrationRole != null && !headerMigrationRole.isBlank()) {
            return headerMigrationRole.trim();
        }
        return "MIGRATION_OPERATOR";
    }
}
