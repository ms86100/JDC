package com.avionics_systems.admin.controller;

import com.avionics_systems.admin.entity.AuditLogEntity;
import com.avionics_systems.admin.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Audit Log Controller - Enterprise audit logging and reporting
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit Log API")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Get audit logs with filters")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAuditLogs(
                userId, category, action, startDate, endDate, page, size));
    }

    @GetMapping("/by-date-range")
    @Operation(summary = "Get audit logs by date range")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAuditLogsByDateRange(start, end, page, size));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Get audit logs by user")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAuditLogsByUser(userId, page, size));
    }

    @GetMapping("/by-category/{category}")
    @Operation(summary = "Get audit logs by category")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLogsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAuditLogsByCategory(category, page, size));
    }

    @GetMapping("/by-entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs by entity")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAuditLogsByEntity(entityType, entityId, page, size));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get audit statistics")
    public ResponseEntity<Map<String, Object>> getAuditStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (start == null) {
            start = LocalDateTime.now().minusDays(30);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        return ResponseEntity.ok(auditService.getAuditStatistics(start, end));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditService.getRecentActivity(limit));
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit logs")
    public ResponseEntity<List<AuditLogEntity>> exportAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Page<AuditLogEntity> page = auditService.getAuditLogs(
                userId, category, null, startDate, endDate, 0, 10000);
        return ResponseEntity.ok(page.getContent());
    }
}
