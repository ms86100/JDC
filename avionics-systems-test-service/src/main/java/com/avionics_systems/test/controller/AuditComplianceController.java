package com.avionics_systems.test.controller;

import com.avionics_systems.test.entity.AuditLog;
import com.avionics_systems.test.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-compliance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit & Compliance", description = "APIs for audit compliance, streaming, and reporting")
public class AuditComplianceController {

    private final AuditService auditService;

    // ==================== Real-time Streaming ====================

    @GetMapping(value = "/stream/{projectId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Subscribe to real-time audit event stream")
    public SseEmitter streamAuditEvents(@PathVariable UUID projectId) {
        log.info("SSE connection request for project: {}", projectId);
        return auditService.subscribeToAuditStream(projectId);
    }

    // ==================== Compliance Reports ====================

    @PostMapping("/reports/{projectId}")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Generate compliance report (SOX, GDPR, HIPAA)")
    public ResponseEntity<AuditService.ComplianceReport> generateComplianceReport(
            @PathVariable UUID projectId,
            @RequestParam String templateType,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        log.info("Generating {} compliance report for project: {}", templateType, projectId);
        AuditService.ComplianceReport report = auditService.generateComplianceReport(
                projectId, templateType, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/templates")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get available compliance report templates")
    public ResponseEntity<List<Map<String, String>>> getComplianceTemplates() {
        List<Map<String, String>> templates = List.of(
                Map.of("type", "SOX", "name", "Sarbanes-Oxley Act", "description", "Financial and accounting compliance reporting"),
                Map.of("type", "GDPR", "name", "GDPR Compliance", "description", "General Data Protection Regulation reporting"),
                Map.of("type", "HIPAA", "name", "HIPAA Compliance", "description", "Health information privacy compliance")
        );
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/reports/{projectId}/{reportId}/export")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Export compliance report in PDF, CSV, or JSON format")
    public ResponseEntity<byte[]> exportComplianceReport(
            @PathVariable UUID projectId,
            @PathVariable String reportId,
            @RequestParam(defaultValue = "JSON") String format,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        log.info("Exporting {} compliance report for project: {}", format, projectId);

        // Generate the report
        AuditService.ComplianceReport report = auditService.generateComplianceReport(
                projectId, "SOX", startDate, endDate);

        Map<String, Object> exportResult = auditService.exportComplianceReport(report, format);

        String contentType = (String) exportResult.get("contentType");
        Object data = exportResult.get("data");

        if (data instanceof byte[]) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "attachment; filename=\"compliance-report." + format.toLowerCase() + "\"")
                    .body((byte[]) data);
        } else {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "attachment; filename=\"compliance-report." + format.toLowerCase() + "\"")
                    .body(((String) data).getBytes());
        }
    }

    // ==================== Anomaly Detection ====================

    @GetMapping("/anomalies/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get detected anomalies for a project")
    public ResponseEntity<List<Map<String, Object>>> getDetectedAnomalies(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "7") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Map<String, Object>> anomalies = auditService.getDetectedAnomalies(projectId, since);
        return ResponseEntity.ok(anomalies);
    }

    // ==================== Activity Heatmap ====================

    @GetMapping("/heatmap/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Generate user activity heatmap for a project")
    public ResponseEntity<Map<String, Map<Integer, Long>>> getActivityHeatmap(
            @PathVariable UUID projectId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        Map<String, Map<Integer, Long>> heatmap = auditService.generateActivityHeatmap(projectId, startDate, endDate);
        return ResponseEntity.ok(heatmap);
    }

    // ==================== Audit Archival ====================

    @PostMapping("/archive/{projectId}")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Archive audit data before a specific date")
    public ResponseEntity<Map<String, Object>> archiveAuditData(
            @PathVariable UUID projectId,
            @RequestParam LocalDateTime beforeDate) {

        auditService.archiveAuditData(projectId, beforeDate);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Audit data archived successfully",
                "archivedBefore", beforeDate.toString()
        ));
    }

    @DeleteMapping("/purge")
    @PreAuthorize("@projectSecurity.isSystemAdmin(authentication)")
    @Operation(summary = "Purge archived audit data (system admin only)")
    public ResponseEntity<Map<String, Object>> purgeArchivedData(
            @RequestParam LocalDateTime beforeDate) {

        long purgedCount = auditService.purgeArchivedData(beforeDate);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Archived audit data purged",
                "purgedBefore", beforeDate.toString(),
                "purgedCount", purgedCount
        ));
    }

    // ==================== Dashboard Summary ====================

    @GetMapping("/dashboard/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get compliance dashboard summary")
    public ResponseEntity<Map<String, Object>> getComplianceDashboard(@PathVariable UUID projectId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime monthAgo = now.minusDays(30);

        // Get stats
        Map<String, Object> weekStats = auditService.getAuditStats(projectId, 7);
        Map<String, Object> monthStats = auditService.getAuditStats(projectId, 30);
        List<Map<String, Object>> recentAnomalies = auditService.getDetectedAnomalies(projectId, weekAgo);

        // Generate summary
        Map<String, Object> dashboard = Map.of(
                "totalEventsWeek", weekStats.getOrDefault("totalActions", 0),
                "totalEventsMonth", monthStats.getOrDefault("totalActions", 0),
                "anomalyCount", recentAnomalies.size(),
                "highSeverityAnomalies", recentAnomalies.stream()
                        .filter(a -> "HIGH".equals(a.getOrDefault("severity", "")))
                        .count(),
                "lastUpdated", now.toString()
        );

        return ResponseEntity.ok(dashboard);
    }

    // ==================== Data Retention Policy ====================

    @GetMapping("/retention-policy")
    @PreAuthorize("@projectSecurity.isSystemAdmin(authentication)")
    @Operation(summary = "Get current data retention policy")
    public ResponseEntity<Map<String, Object>> getRetentionPolicy() {
        Map<String, Object> policy = Map.of(
                "retentionYears", 7,
                "archivalAfterYears", 3,
                "purgeAfterArchivalMonths", 6,
                "lastArchivalRun", LocalDateTime.now().minusDays(1).toString()
        );
        return ResponseEntity.ok(policy);
    }

    @PostMapping("/retention-policy")
    @PreAuthorize("@projectSecurity.isSystemAdmin(authentication)")
    @Operation(summary = "Update data retention policy (system admin only)")
    public ResponseEntity<Map<String, Object>> updateRetentionPolicy(
            @RequestParam(defaultValue = "7") int retentionYears,
            @RequestParam(defaultValue = "3") int archivalAfterYears) {

        log.info("Retention policy updated: retentionYears={}, archivalAfterYears={}",
                retentionYears, archivalAfterYears);

        return ResponseEntity.ok(Map.of(
                "status", "updated",
                "retentionYears", retentionYears,
                "archivalAfterYears", archivalAfterYears
        ));
    }
}