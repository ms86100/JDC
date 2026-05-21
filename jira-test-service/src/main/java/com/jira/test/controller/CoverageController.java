package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.CoverageReportService;
import com.jira.test.service.CoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/coverage")
@RequiredArgsConstructor
@Tag(name = "Coverage Engine", description = "APIs for coverage rules, thresholds, trends, and analytics")
public class CoverageController {

    private final CoverageService coverageService;
    private final CoverageReportService coverageReportService;

    @GetMapping("/{projectId}")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get project coverage summary")
    public ResponseEntity<ProjectCoverageResponse> getProjectCoverage(@PathVariable UUID projectId) {
        ProjectCoverageResponse coverage = coverageService.getProjectCoverage(projectId);
        return ResponseEntity.ok(coverage);
    }

    @GetMapping("/{projectId}/trend")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get coverage trends over time")
    public ResponseEntity<CoverageTrendResponse> getCoverageTrends(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "30") int days) {
        CoverageTrendResponse trends = coverageService.getCoverageTrends(projectId, days);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/{projectId}/requirements")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get coverage by requirement")
    public ResponseEntity<List<CoverageThresholdResponse>> getCoverageByRequirements(@PathVariable UUID projectId) {
        List<CoverageThresholdResponse> thresholds = coverageService.getThresholdsByProject(projectId);
        return ResponseEntity.ok(thresholds);
    }

    @GetMapping("/{projectId}/matrix")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get coverage matrix")
    public ResponseEntity<CoverageMatrixResponse> getCoverageMatrix(@PathVariable UUID projectId) {
        CoverageMatrixResponse matrix = coverageService.getCoverageMatrix(projectId);
        return ResponseEntity.ok(matrix);
    }

    @GetMapping("/{projectId}/suggestions")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get automated coverage suggestions")
    public ResponseEntity<CoverageSuggestionResponse> getSuggestions(@PathVariable UUID projectId) {
        CoverageSuggestionResponse suggestions = coverageService.getSuggestions(projectId);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/{projectId}/rules")
    @PreAuthorize("@projectSecurity.canManageProject(authentication, #projectId)")
    @Operation(summary = "List coverage rules for a project")
    public ResponseEntity<List<CoverageRuleResponse>> getRules(@PathVariable UUID projectId) {
        List<CoverageRuleResponse> rules = coverageService.getRulesByProject(projectId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{projectId}/rules/enabled")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "List enabled coverage rules for a project")
    public ResponseEntity<List<CoverageRuleResponse>> getEnabledRules(@PathVariable UUID projectId) {
        List<CoverageRuleResponse> rules = coverageService.getEnabledRules(projectId);
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/rules")
    @PreAuthorize("@projectSecurity.canManageProject(authentication, #request.projectId)")
    @Operation(summary = "Create a coverage rule")
    public ResponseEntity<CoverageRuleResponse> createRule(@RequestBody CoverageRuleRequest request) {
        CoverageRuleResponse rule = coverageService.createRule(request);
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("@projectSecurity.canManageProject(authentication, #projectId)")
    @Operation(summary = "Update a coverage rule")
    public ResponseEntity<CoverageRuleResponse> updateRule(
            @PathVariable UUID ruleId,
            @RequestBody CoverageRuleRequest request) {
        CoverageRuleResponse rule = coverageService.updateRule(ruleId, request);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("@projectSecurity.canManageProject(authentication, #projectId)")
    @Operation(summary = "Delete a coverage rule")
    public ResponseEntity<Void> deleteRule(
            @PathVariable UUID ruleId,
            @RequestParam UUID projectId) {
        coverageService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/thresholds")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "List coverage thresholds for a project")
    public ResponseEntity<List<CoverageThresholdResponse>> getThresholds(@PathVariable UUID projectId) {
        List<CoverageThresholdResponse> thresholds = coverageService.getThresholdsByProject(projectId);
        return ResponseEntity.ok(thresholds);
    }

    @PutMapping("/thresholds/{requirementId}")
    @PreAuthorize("@projectSecurity.canManageProject(authentication, #request.projectId)")
    @Operation(summary = "Update coverage threshold for a requirement")
    public ResponseEntity<CoverageThresholdResponse> updateThreshold(
            @PathVariable UUID requirementId,
            @RequestBody CoverageThresholdRequest request) {
        CoverageThresholdResponse threshold = coverageService.updateThreshold(requirementId, request);
        return ResponseEntity.ok(threshold);
    }

    @GetMapping("/{projectId}/alerts")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get threshold alerts and rule violations")
    public ResponseEntity<List<CoverageService.CoverageAlertResponse>> getAlerts(@PathVariable UUID projectId) {
        List<CoverageService.CoverageAlertResponse> alerts = coverageService.getAlerts(projectId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{projectId}/violations")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Evaluate coverage rules and return violations")
    public ResponseEntity<List<CoverageService.CoverageRuleViolation>> evaluateRules(@PathVariable UUID projectId) {
        List<CoverageService.CoverageRuleViolation> violations = coverageService.evaluateRules(projectId);
        return ResponseEntity.ok(violations);
    }

    // ========== EXPORT & REPORTING ENDPOINTS ==========

    @GetMapping("/rules/{projectId}")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get coverage rules for a project")
    public ResponseEntity<List<CoverageRuleResponse>> getRulesByProject(@PathVariable UUID projectId) {
        List<CoverageRuleResponse> rules = coverageService.getRulesByProject(projectId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/alerts/{projectId}")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get coverage alerts for a project")
    public ResponseEntity<List<CoverageService.CoverageAlertResponse>> getAlertsByProject(@PathVariable UUID projectId) {
        List<CoverageService.CoverageAlertResponse> alerts = coverageService.getAlerts(projectId);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/reports/generate")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #request.projectId)")
    @Operation(summary = "Generate a coverage report")
    public ResponseEntity<CoverageExportResponse> generateReport(@RequestBody CoverageExportRequest request) {
        CoverageExportResponse report = coverageReportService.generateReport(request);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/{projectId}/export")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Export coverage report in specified format")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "JSON") CoverageExportRequest.ExportFormat format,
            @RequestParam(defaultValue = "EXECUTIVE_SUMMARY") CoverageExportRequest.ReportType reportType,
            @RequestParam(defaultValue = "30") int periodDays) {

        CoverageExportRequest request = CoverageExportRequest.builder()
                .projectId(projectId)
                .format(format)
                .reportType(reportType)
                .periodDays(periodDays)
                .build();

        byte[] data = coverageReportService.exportReport(request);
        String filename = String.format("coverage_report_%s.%s", reportType.name().toLowerCase(),
                format.name().toLowerCase());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                format == CoverageExportRequest.ExportFormat.JSON ? "application/json" :
                format == CoverageExportRequest.ExportFormat.CSV ? "text/csv" :
                format == CoverageExportRequest.ExportFormat.PDF ? "application/pdf" :
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/reports/{projectId}/summary")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get report summary for a project")
    public ResponseEntity<Map<String, Object>> getReportSummary(@PathVariable UUID projectId) {
        Map<String, Object> summary = coverageReportService.getReportSummary(projectId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/reports/types")
    @PreAuthorize("@projectSecurity.canViewProject(authentication, #projectId)")
    @Operation(summary = "Get available report types")
    public ResponseEntity<List<Map<String, Object>>> getAvailableReportTypes() {
        List<Map<String, Object>> types = coverageReportService.getAvailableReportTypes();
        return ResponseEntity.ok(types);
    }
}