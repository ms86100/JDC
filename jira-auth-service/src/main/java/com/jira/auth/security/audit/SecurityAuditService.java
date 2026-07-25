package com.jira.auth.security.audit;

import com.jira.auth.security.audit.SecurityAuditEvent.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Security Audit Service
 * Phase 7 - Polish & Performance
 * Manages security audit trail and suspicious activity detection
 */
@Service
@Slf4j
public class SecurityAuditService {

    // In-memory audit log (in production, this would be persisted to a database)
    private final Map<UUID, List<SecurityAuditEvent>> userAuditLog = new ConcurrentHashMap<>();
    private final Map<String, FailedAttemptTracker> failedAttemptsByIp = new ConcurrentHashMap<>();
    private final MessageSource messageSource;

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    @Value("${app.security.suspicious-activity-threshold:3}")
    private int suspiciousActivityThreshold;

    public SecurityAuditService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Record a security event
     */
    public void recordEvent(SecurityAuditEvent event) {
        log.info("Security audit event: {} for user {} from IP {}",
                event.getEventType(), event.getUsername(), event.getIpAddress());

        // Store in user audit log
        if (event.getUserId() != null) {
            userAuditLog.computeIfAbsent(event.getUserId(), k ->
                    new java.util.concurrent.CopyOnWriteArrayList<>()).add(event);
        }

        // Track failed attempts for suspicious activity detection
        if (event.getEventType() == EventType.LOGIN_FAILED) {
            trackFailedLogin(event.getIpAddress(), event.getUsername());
        }

        // Check for suspicious activity patterns
        if (isSuspiciousActivity(event)) {
            log.warn("Suspicious activity detected for user {} from IP {}",
                    event.getUsername(), event.getIpAddress());
        }
    }

    /**
     * Record login success
     */
    public void recordLoginSuccess(UUID userId, String username, String ipAddress, String userAgent) {
        recordEvent(SecurityAuditEvent.builder()
                .userId(userId)
                .username(username)
                .eventType(EventType.LOGIN_SUCCESS)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .build());

        // Clear failed attempts on successful login
        clearFailedAttempts(ipAddress);
    }

    /**
     * Record login failure
     */
    public void recordLoginFailed(String username, String ipAddress, String reason) {
        recordEvent(SecurityAuditEvent.builder()
                .username(username)
                .eventType(EventType.LOGIN_FAILED)
                .ipAddress(ipAddress)
                .success(false)
                .failureReason(reason)
                .build());
    }

    /**
     * Record access denied
     */
    public void recordAccessDenied(UUID userId, String username, String ipAddress,
                                   String resource, String action) {
        recordEvent(SecurityAuditEvent.builder()
                .userId(userId)
                .username(username)
                .eventType(EventType.ACCESS_DENIED)
                .ipAddress(ipAddress)
                .resource(resource)
                .action(action)
                .success(false)
                .failureReason(messageSource.getMessage("audit.failure.insufficient.permissions", null, Locale.ENGLISH))
                .build());
    }

    /**
     * Record invalid token usage
     */
    public void recordInvalidToken(String ipAddress, String tokenPreview, String reason) {
        recordEvent(SecurityAuditEvent.builder()
                .eventType(EventType.INVALID_TOKEN)
                .ipAddress(ipAddress)
                .additionalDetails(messageSource.getMessage("audit.token.details",
                        new Object[]{tokenPreview, reason}, Locale.ENGLISH))
                .success(false)
                .failureReason(reason)
                .build());
    }

    /**
     * Check if IP is locked out due to too many failed attempts
     */
    public boolean isIpLockedOut(String ipAddress) {
        FailedAttemptTracker tracker = failedAttemptsByIp.get(ipAddress);
        if (tracker == null) {
            return false;
        }

        if (tracker.isExpired()) {
            failedAttemptsByIp.remove(ipAddress);
            return false;
        }

        return tracker.getFailedAttempts() >= maxFailedLoginAttempts;
    }

    /**
     * Get audit log for a specific user
     */
    public List<SecurityAuditEvent> getUserAuditLog(UUID userId, OffsetDateTime since) {
        return userAuditLog.getOrDefault(userId, List.of()).stream()
                .filter(e -> since == null || e.getTimestamp().isAfter(since))
                .collect(Collectors.toList());
    }

    /**
     * Get recent audit events
     */
    public List<SecurityAuditEvent> getRecentEvents(int limit) {
        return userAuditLog.values().stream()
                .flatMap(List::stream)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get security summary
     */
    public SecuritySummary getSecuritySummary() {
        long totalEvents = userAuditLog.values().stream().mapToLong(List::size).sum();
        long failedLogins = countEventsByType(EventType.LOGIN_FAILED);
        long accessDenied = countEventsByType(EventType.ACCESS_DENIED);
        long invalidTokens = countEventsByType(EventType.INVALID_TOKEN);
        long lockedOutIps = failedAttemptsByIp.values().stream()
                .filter(t -> t.getFailedAttempts() >= maxFailedLoginAttempts)
                .count();

        return new SecuritySummary(
                totalEvents,
                failedLogins,
                accessDenied,
                invalidTokens,
                lockedOutIps,
                failedAttemptsByIp.size()
        );
    }

    private void trackFailedLogin(String ipAddress, String username) {
        failedAttemptsByIp.computeIfAbsent(ipAddress, k -> new FailedAttemptTracker(lockoutDurationMinutes))
                .incrementFailedAttempts();
    }

    private void clearFailedAttempts(String ipAddress) {
        failedAttemptsByIp.remove(ipAddress);
    }

    private boolean isSuspiciousActivity(SecurityAuditEvent event) {
        // Check for rapid failed login attempts
        if (event.getEventType() == EventType.LOGIN_FAILED) {
            FailedAttemptTracker tracker = failedAttemptsByIp.get(event.getIpAddress());
            return tracker != null && tracker.getFailedAttempts() > suspiciousActivityThreshold;
        }

        // Check for unusual patterns (multiple failed logins for different users from same IP)
        return false;
    }

    private long countEventsByType(EventType type) {
        return userAuditLog.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.getEventType() == type)
                .count();
    }

    /**
     * Track failed login attempts per IP
     */
    private static class FailedAttemptTracker {
        private int failedAttempts = 0;
        private OffsetDateTime firstAttempt;
        private final int windowMinutes;

        public FailedAttemptTracker(int windowMinutes) {
            this.firstAttempt = OffsetDateTime.now();
            this.windowMinutes = windowMinutes;
        }

        public void incrementFailedAttempts() {
            if (isExpired()) {
                failedAttempts = 0;
                firstAttempt = OffsetDateTime.now();
            }
            failedAttempts++;
        }

        public int getFailedAttempts() {
            return isExpired() ? 0 : failedAttempts;
        }

        public boolean isExpired() {
            return firstAttempt.plusMinutes(windowMinutes).isBefore(OffsetDateTime.now());
        }
    }

    /**
     * Security summary record
     */
    public record SecuritySummary(
            long totalEvents,
            long failedLogins,
            long accessDenied,
            long invalidTokens,
            long lockedOutIps,
            long trackedIps
    ) {}
}
