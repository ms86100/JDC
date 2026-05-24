package com.jira.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security Audit Service for tracking authentication events.
 * Provides audit logging for security monitoring and compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    // In-memory audit log (in production, use a database or external audit service)
    private final Map<UUID, AuditEntry> auditLog = new ConcurrentHashMap<>();

    public enum AuditEvent {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        TOKEN_REFRESH,
        TOKEN_REVOCATION,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        SUSPICIOUS_ACTIVITY,
        PERMISSION_DENIED,
        SESSION_EXPIRED
    }

    public record AuditEntry(
            UUID userId,
            AuditEvent event,
            String details,
            String ipAddress,
            String userAgent,
            Instant timestamp,
            boolean success
    ) {}

    /**
     * Log a security event.
     */
    public void logEvent(UUID userId, AuditEvent event, String details,
                         String ipAddress, String userAgent, boolean success) {
        AuditEntry entry = new AuditEntry(
                userId,
                event,
                details,
                ipAddress,
                userAgent,
                Instant.now(),
                success
        );

        auditLog.put(UUID.randomUUID(), entry);

        // Log to application log as well
        if (success) {
            log.info("Security audit [{}]: user={}, event={}, ip={}",
                    event, userId, event, ipAddress);
        } else {
            log.warn("Security audit [{}]: user={}, event={}, ip={}, details={}",
                    event, userId, event, ipAddress, details);
        }
    }

    /**
     * Log successful login.
     */
    public void logLoginSuccess(UUID userId, String ipAddress, String userAgent) {
        logEvent(userId, AuditEvent.LOGIN_SUCCESS, "User logged in successfully",
                ipAddress, userAgent, true);
    }

    /**
     * Log failed login attempt.
     */
    public void logLoginFailure(String identifier, String ipAddress, String reason) {
        logEvent(null, AuditEvent.LOGIN_FAILURE,
                "Login failed: " + reason + " for identifier: " + maskIdentifier(identifier),
                ipAddress, null, false);
    }

    /**
     * Log token refresh.
     */
    public void logTokenRefresh(UUID userId, String ipAddress) {
        logEvent(userId, AuditEvent.TOKEN_REFRESH, "Token refreshed successfully",
                ipAddress, null, true);
    }

    /**
     * Log permission denied event.
     */
    public void logPermissionDenied(UUID userId, String resource, String action,
                                    String ipAddress) {
        logEvent(userId, AuditEvent.PERMISSION_DENIED,
                "Access denied to " + resource + " for action: " + action,
                ipAddress, null, false);
    }

    /**
     * Log suspicious activity.
     */
    public void logSuspiciousActivity(UUID userId, String description,
                                       String ipAddress, String userAgent) {
        logEvent(userId, AuditEvent.SUSPICIOUS_ACTIVITY, description,
                ipAddress, userAgent, false);
    }

    /**
     * Get audit log for a specific user.
     */
    public java.util.List<AuditEntry> getAuditLogForUser(UUID userId) {
        return auditLog.values().stream()
                .filter(entry -> userId.equals(entry.userId()))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get recent audit entries.
     */
    public java.util.List<AuditEntry> getRecentEntries(int limit) {
        return auditLog.values().stream()
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get failed login attempts for a specific IP address.
     */
    public java.util.List<AuditEntry> getFailedLoginsForIp(String ipAddress, int minutes) {
        Instant cutoff = Instant.now().minusSeconds(minutes * 60L);

        return auditLog.values().stream()
                .filter(entry -> ipAddress.equals(entry.ipAddress()))
                .filter(entry -> entry.event() == AuditEvent.LOGIN_FAILURE)
                .filter(entry -> entry.timestamp().isAfter(cutoff))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Check if an IP has too many failed login attempts (for rate limiting).
     */
    public int getFailedLoginCount(String ipAddress, int minutes) {
        return getFailedLoginsForIp(ipAddress, minutes).size();
    }

    /**
     * Get security summary for admin dashboard.
     */
    public Map<String, Object> getSecuritySummary() {
        Instant dayAgo = Instant.now().minusSeconds(86400L);

        long totalEvents = auditLog.values().stream().count();
        long recentEvents = auditLog.values().stream()
                .filter(e -> e.timestamp().isAfter(dayAgo)).count();
        long failedLogins = auditLog.values().stream()
                .filter(e -> e.event() == AuditEvent.LOGIN_FAILURE)
                .filter(e -> e.timestamp().isAfter(dayAgo)).count();
        long suspiciousActivities = auditLog.values().stream()
                .filter(e -> e.event() == AuditEvent.SUSPICIOUS_ACTIVITY)
                .filter(e -> e.timestamp().isAfter(dayAgo)).count();
        long permissionDenied = auditLog.values().stream()
                .filter(e -> e.event() == AuditEvent.PERMISSION_DENIED)
                .filter(e -> e.timestamp().isAfter(dayAgo)).count();

        return Map.of(
                "total_audit_entries", totalEvents,
                "events_last_24h", recentEvents,
                "failed_logins_last_24h", failedLogins,
                "suspicious_activities_last_24h", suspiciousActivities,
                "permission_denied_last_24h", permissionDenied,
                "audit_log_size", auditLog.size()
        );
    }

    /**
     * Clear old audit entries (retention policy).
     */
    public void clearOldEntries(int retentionDays) {
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86400L);

        auditLog.entrySet().removeIf(entry ->
                entry.getValue().timestamp().isBefore(cutoff));

        log.info("Cleared audit entries older than {} days", retentionDays);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 4) {
            return "****";
        }
        // Show only last 4 characters
        return "****" + identifier.substring(identifier.length() - 4);
    }
}