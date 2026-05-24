package com.jira.auth.controller;

import com.jira.auth.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Security audit controller for monitoring authentication events.
 * Provides endpoints for security monitoring and compliance.
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Security Audit", description = "Security audit and monitoring endpoints")
public class SecurityAuditController {

    private final SecurityAuditService securityAuditService;

    @GetMapping("/events")
    @Operation(summary = "Get recent audit events", description = "Get recent security audit events")
    public ResponseEntity<List<SecurityAuditService.AuditEntry>> getRecentEvents(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(securityAuditService.getRecentEntries(limit));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user audit log", description = "Get audit log for a specific user")
    public ResponseEntity<List<SecurityAuditService.AuditEntry>> getUserAuditLog(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(securityAuditService.getAuditLogForUser(userId));
    }

    @GetMapping("/failed-logins")
    @Operation(summary = "Get failed login attempts", description = "Get failed login attempts for an IP address")
    public ResponseEntity<List<SecurityAuditService.AuditEntry>> getFailedLogins(
            @RequestParam String ipAddress,
            @RequestParam(defaultValue = "30") int minutes) {
        return ResponseEntity.ok(securityAuditService.getFailedLoginsForIp(ipAddress, minutes));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get security summary", description = "Get security summary for admin dashboard")
    public ResponseEntity<Map<String, Object>> getSecuritySummary() {
        return ResponseEntity.ok(securityAuditService.getSecuritySummary());
    }

    @PostMapping("/clear-old")
    @Operation(summary = "Clear old audit entries", description = "Clear audit entries older than retention period")
    public ResponseEntity<Map<String, Object>> clearOldEntries(
            @RequestParam(defaultValue = "90") int retentionDays) {
        securityAuditService.clearOldEntries(retentionDays);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cleared audit entries older than " + retentionDays + " days"
        ));
    }
}