package com.avionics_systems.audit.controller;

import com.avionics_systems.audit.dto.AuditEvent;
import com.avionics_systems.audit.dto.AuditLogResponse;
import com.avionics_systems.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit log management")
public class AuditController {

    private final AuditService auditService;

    @Value("${app.audit.default-page-size:20}")
    private int defaultPageSize;

    @PostMapping("/logs")
    @Operation(summary = "Create an audit log entry (internal service call)")
    public ResponseEntity<AuditLogResponse> createLog(@RequestBody AuditEvent event) {
        return ResponseEntity.ok(auditService.logEvent(event));
    }

    @GetMapping("/logs")
    @Operation(summary = "Search audit logs with filters")
    public ResponseEntity<Page<AuditLogResponse>> searchLogs(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int effectiveSize = (size != null) ? size : defaultPageSize;
        return ResponseEntity.ok(auditService.searchLogs(serviceName, entityType, entityId, userId, action, page, effectiveSize));
    }

    @GetMapping("/logs/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for a specific entity")
    public ResponseEntity<Page<AuditLogResponse>> getLogsForEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int effectiveSize = (size != null) ? size : defaultPageSize;
        return ResponseEntity.ok(auditService.getLogsForEntity(entityType, entityId, page, effectiveSize));
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<Page<AuditLogResponse>> getLogsForUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int effectiveSize = (size != null) ? size : defaultPageSize;
        return ResponseEntity.ok(auditService.getLogsForUser(userId, page, effectiveSize));
    }
}
