package com.avionics_systems.test.controller;

import com.avionics_systems.test.entity.AuditLog;
import com.avionics_systems.test.enums.AuditAction;
import com.avionics_systems.test.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit & Compliance", description = "APIs for audit logging and compliance reports")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get audit logs by project")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditService.getAuditLogs(projectId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #entityId)")
    @Operation(summary = "Get entity history")
    public ResponseEntity<Page<AuditLog>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditService.getAuditLogsByEntity(entityType, entityId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity-history/{entityType}/{entityId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #entityId)")
    @Operation(summary = "Get complete entity history (no pagination)")
    public ResponseEntity<List<AuditLog>> getCompleteEntityHistory(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        List<AuditLog> logs = auditService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #userId)")
    @Operation(summary = "Get user activity")
    public ResponseEntity<Page<AuditLog>> getUserActivity(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditService.getAuditLogsByUser(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/recent/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get recent activity for a project")
    public ResponseEntity<List<AuditLog>> getRecentActivity(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "7") int days) {
        List<AuditLog> logs = auditService.getRecentActivity(projectId, days);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get audit statistics for a project")
    public ResponseEntity<Map<String, Object>> getAuditStats(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> stats = auditService.getAuditStats(projectId, days);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/search")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Search audit logs with filters")
    public ResponseEntity<List<AuditLog>> searchAuditLogs(
            @RequestParam UUID projectId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<AuditLog> logs = auditService.searchAuditLogs(projectId, action, startDate, endDate, pageable);
        return ResponseEntity.ok(logs);
    }
}