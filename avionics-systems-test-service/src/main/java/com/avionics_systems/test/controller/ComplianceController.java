package com.avionics_systems.test.controller;

import com.avionics_systems.test.service.ComplianceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance Reports", description = "APIs for compliance reporting and data export")
public class ComplianceController {

    private final ComplianceReportService complianceReportService;

    @GetMapping("/report/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Generate compliance report for a project")
    public ResponseEntity<Map<String, Object>> generateComplianceReport(
            @PathVariable UUID projectId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {

        // Default to last 30 days if not specified
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        Map<String, Object> report = complianceReportService.generateComplianceReport(projectId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/user-activity/{projectId}/{userId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get user activity report")
    public ResponseEntity<List<Map<String, Object>>> getUserActivityReport(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> report = complianceReportService.getUserActivityReport(projectId, userId, days);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/user-summary/{projectId}/{userId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get user activity summary")
    public ResponseEntity<Map<String, Object>> getUserActivitySummary(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> summary = complianceReportService.getUserActivitySummary(projectId, userId, days);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/export/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Export compliance data")
    public ResponseEntity<String> exportComplianceData(
            @PathVariable UUID projectId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "JSON") String format) {

        // Default to last 30 days if not specified
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        String data = complianceReportService.exportComplianceData(projectId, startDate, endDate, format);

        if ("CSV".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compliance-export.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(data);
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compliance-export.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(data);
        }
    }
}