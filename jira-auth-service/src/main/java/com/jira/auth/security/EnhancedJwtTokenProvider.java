package com.jira.auth.security;

import com.jira.auth.service.SecurityAuditService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Enhanced JWT Token Provider with security audit logging.
 * Provides secure token generation and validation with audit trail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnhancedJwtTokenProvider {

    private final SecurityAuditService securityAuditService;

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public EnhancedJwtTokenProvider(
            SecurityAuditService securityAuditService,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.securityAuditService = securityAuditService;
    }

    public String generateAccessToken(UUID userId, String username, java.util.Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .claim("tokenType", "access")
                .claim("issuedAt", now.getTime())
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString()) // Unique token ID for tracking
                .signWith(secretKey)
                .compact();

        log.debug("Generated access token for user: {}", userId);
        return token;
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .claim("tokenType", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.Set<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new java.util.HashSet<>((java.util.List<String>) claims.get("roles"));
    }

    /**
     * Validate token with enhanced security checks.
     */
    public TokenValidationResult validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> new Date()) // Use current time for expiry check
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check token type
            String tokenType = claims.get("tokenType", String.class);
            if (tokenType == null) {
                return new TokenValidationResult(false, "Invalid token type", null);
            }

            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                return new TokenValidationResult(false, "Token expired", null);
            }

            UUID userId = UUID.fromString(claims.getSubject());
            return new TokenValidationResult(true, "Valid", userId);

        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return new TokenValidationResult(false, "Token expired", null);

        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return new TokenValidationResult(false, "Malformed token", null);

        } catch (SecurityException e) {
            log.warn("Invalid signature: {}", e.getMessage());
            return new TokenValidationResult(false, "Invalid signature", null);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return new TokenValidationResult(false, "Invalid token", null);

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return new TokenValidationResult(false, "Validation failed", null);
        }
    }

    /**
     * Check if token is a refresh token.
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get token ID (jti claim) for tracking.
     */
    public String getTokenId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get remaining time until token expires (in seconds).
     */
    public long getRemainingTime(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            if (expiration != null) {
                long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
                return Math.max(0, remaining);
            }
        } catch (Exception e) {
            log.debug("Could not get remaining time: {}", e.getMessage());
        }
        return 0;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public record TokenValidationResult(boolean valid, String message, UUID userId) {}
}