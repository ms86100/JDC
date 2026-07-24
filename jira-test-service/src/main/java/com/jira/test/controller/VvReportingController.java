package com.jira.test.controller;

import com.jira.test.dto.BenchDefectSummaryReport;
import com.jira.test.dto.ProblemReportSummaryReport;
import com.jira.test.dto.ProjectDashboardResponse;
import com.jira.test.dto.TechEventSummaryReport;
import com.jira.test.dto.VvoCoverageReport;
import com.jira.test.service.VvReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vv-reports")
@RequiredArgsConstructor
@Tag(name = "V&V Reporting", description = "SYSDOPS V&V coverage, defect, and dashboard reports")
public class VvReportingController {

    private final VvReportingService reportingService;

    // === VVO Coverage ===

    @GetMapping("/coverage")
    @Operation(summary = "VVO coverage report with linked tests by component")
    public ResponseEntity<VvoCoverageReport> getCoverageReport(
            @RequestParam UUID projectId,
            @RequestParam UUID fixVersionId) {
        return ResponseEntity.ok(reportingService.generateCoverageReport(projectId, fixVersionId));
    }

    @GetMapping("/coverage/export")
    @Operation(summary = "Export VVO coverage report as CSV")
    public ResponseEntity<String> exportCoverageReportCsv(
            @RequestParam UUID projectId,
            @RequestParam UUID fixVersionId) {
        String csv = reportingService.exportCoverageReportCsv(projectId, fixVersionId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=vvo_coverage.csv")
                .body(csv);
    }

    // === TechEvent Report ===

    @GetMapping("/tech-events")
    @Operation(summary = "TechEvent summary report with counts by status/type/origin/impact")
    public ResponseEntity<TechEventSummaryReport> getTechEventReport(
            @RequestParam UUID projectId) {
        return ResponseEntity.ok(reportingService.generateTechEventReport(projectId));
    }

    // === Bench Defect Report ===

    @GetMapping("/bench-defects")
    @Operation(summary = "Bench Defect summary report with counts by status/severity")
    public ResponseEntity<BenchDefectSummaryReport> getBenchDefectReport(
            @RequestParam UUID projectId) {
        return ResponseEntity.ok(reportingService.generateBenchDefectReport(projectId));
    }

    // === Problem Report ===

    @GetMapping("/problem-reports")
    @Operation(summary = "Problem Report summary with counts by status/type/origin")
    public ResponseEntity<ProblemReportSummaryReport> getProblemReportSummary(
            @RequestParam UUID projectId) {
        return ResponseEntity.ok(reportingService.generateProblemReportSummary(projectId));
    }

    // === Project Dashboard ===

    @GetMapping("/dashboard/{projectId}")
    @Operation(summary = "Project V&V dashboard — aggregated metrics across all artifact types")
    public ResponseEntity<ProjectDashboardResponse> getProjectDashboard(
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(reportingService.getProjectDashboard(projectId));
    }

    // === Export for Planning ===

    @GetMapping("/planning-export/{testPlanId}")
    @Operation(summary = "Export for planning — estimated duration by component/test means/priority")
    public ResponseEntity<String> exportForPlanning(
            @PathVariable UUID testPlanId) {
        String csv = reportingService.exportForPlanning(testPlanId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=planning_export.csv")
                .body(csv);
    }
}
