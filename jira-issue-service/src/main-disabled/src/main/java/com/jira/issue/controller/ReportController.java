package com.jira.issue.controller;

import com.jira.issue.dto.*;
import com.jira.issue.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Report Controller - Test analytics and reporting
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Test reporting and analytics APIs")
public class ReportController {

    private final ReportingService reportService;

    @GetMapping("/summary")
    @Operation(summary = "Get test execution summary report")
    public ResponseEntity<ReportSummaryResponse> getSummaryReport(
            @RequestParam UUID projectId,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getSummaryReport(projectId, sprintId, startDate, endDate));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get test execution trends")
    public ResponseEntity<List<TestTrendResponse>> getTestTrends(
            @RequestParam UUID projectId,
            @RequestParam UUID testId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(reportService.getTestTrends(projectId, testId, days));
    }

    @GetMapping("/coverage")
    @Operation(summary = "Get requirement coverage report")
    public ResponseEntity<RequirementCoverageResponse> getCoverageReport(@RequestParam UUID projectId) {
        return ResponseEntity.ok(reportService.getRequirementCoverageReport(projectId));
    }

    @GetMapping("/defect-density")
    @Operation(summary = "Get defect density report")
    public ResponseEntity<DefectDensityResponse> getDefectDensityReport(@RequestParam UUID projectId) {
        return ResponseEntity.ok(reportService.getDefectDensityReport(projectId));
    }

    @GetMapping("/sprint-quality")
    @Operation(summary = "Get sprint quality report")
    public ResponseEntity<SprintQualityResponse> getSprintQualityReport(
            @RequestParam UUID projectId,
            @RequestParam(required = false) UUID sprintId) {
        return ResponseEntity.ok(reportService.getSprintQualityReport(projectId, sprintId));
    }

    @GetMapping("/automation-coverage")
    @Operation(summary = "Get automation coverage report")
    public ResponseEntity<AutomationCoverageResponse> getAutomationCoverageReport(@RequestParam UUID projectId) {
        return ResponseEntity.ok(reportService.getAutomationCoverageReport(projectId));
    }
}