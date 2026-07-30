package com.avionics_systems.auth.security.audit;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Security Audit Event
 * Phase 7 - Polish & Performance
 * Records security-related events for audit trail
 */
@Data
@Builder
public class SecurityAuditEvent {

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        TOKEN_REFRESH,
        TOKEN_EXPIRED,
        ACCESS_DENIED,
        INVALID_TOKEN,
        PASSWORD_CHANGED,
        PASSWORD_RESET_REQUESTED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        SUSPICIOUS_ACTIVITY,
        PERMISSION_DENIED,
        PRIVILEGE_ESCALATION_ATTEMPT
    }

    private UUID userId;
    private String username;
    private EventType eventType;
    private String ipAddress;
    private String userAgent;
    private String resource;
    private String action;
    private boolean success;
    private String failureReason;
    private String sessionId;
    private OffsetDateTime timestamp;
    private String additionalDetails;

    public static SecurityAuditEventBuilder builder() {
        return new SecurityAuditEventBuilder()
                .timestamp(OffsetDateTime.now());
    }
}