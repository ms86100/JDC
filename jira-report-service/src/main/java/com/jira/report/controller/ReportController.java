package com.jira.report.controller;

import com.jira.report.dto.*;
import com.jira.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation and management endpoints")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    // Time Tracking Reports
    @PostMapping("/time-tracking")
    @Operation(summary = "Generate time tracking report", description = "Creates a time tracking report for specified date range")
    public ResponseEntity<TimeTrackingReportResponse> generateTimeTrackingReport(
            @Valid @RequestBody TimeTrackingReportRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.generateTimeTrackingReport(request, actor));
    }

    @GetMapping("/time-tracking")
    @Operation(summary = "Get user's time tracking reports", description = "Returns all time tracking reports for the current user")
    public ResponseEntity<List<TimeTrackingReportResponse>> getTimeTrackingReports(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.ok(reportService.getTimeTrackingReportsByUser(actor));
    }

    @GetMapping("/time-tracking/{id}")
    @Operation(summary = "Get time tracking report by ID", description = "Returns a specific time tracking report")
    public ResponseEntity<TimeTrackingReportResponse> getTimeTrackingReport(
            @Parameter(description = "Report ID") @PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getTimeTrackingReport(id));
    }

    // Sprint Reports
    @PostMapping("/sprint")
    @Operation(summary = "Generate sprint report", description = "Creates a sprint report for specified sprint")
    public ResponseEntity<SprintReportResponse> generateSprintReport(
            @Valid @RequestBody GenerateSprintReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.generateSprintReport(request));
    }

    @GetMapping("/sprint/{id}")
    @Operation(summary = "Get sprint report by ID", description = "Returns a specific sprint report")
    public ResponseEntity<SprintReportResponse> getSprintReport(
            @Parameter(description = "Report ID") @PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getSprintReport(id));
    }

    @GetMapping("/sprint/by-sprint/{sprintId}")
    @Operation(summary = "Get sprint reports by sprint", description = "Returns all reports for a specific sprint")
    public ResponseEntity<List<SprintReportResponse>> getSprintReportsBySprint(
            @Parameter(description = "Sprint ID") @PathVariable UUID sprintId) {
        return ResponseEntity.ok(reportService.getSprintReportsBySprint(sprintId));
    }

    // Project Reports
    @PostMapping("/project")
    @Operation(summary = "Generate project report", description = "Creates a project report for specified project")
    public ResponseEntity<ProjectReportResponse> generateProjectReport(
            @Valid @RequestBody GenerateProjectReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.generateProjectReport(request));
    }

    @GetMapping("/project/{id}")
    @Operation(summary = "Get project report by ID", description = "Returns a specific project report")
    public ResponseEntity<ProjectReportResponse> getProjectReport(
            @Parameter(description = "Report ID") @PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getProjectReport(id));
    }

    @GetMapping("/project/by-project/{projectId}")
    @Operation(summary = "Get project reports", description = "Returns all reports for a specific project")
    public ResponseEntity<List<ProjectReportResponse>> getProjectReportsByProject(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {
        return ResponseEntity.ok(reportService.getProjectReportsByProject(projectId));
    }

    // Saved Reports
    @PostMapping("/saved")
    @Operation(summary = "Save a report configuration", description = "Saves a report configuration for later use")
    public ResponseEntity<SavedReportResponse> saveReport(
            @Valid @RequestBody SaveReportRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.saveReport(request, actor));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get saved reports", description = "Returns all saved reports for the current user")
    public ResponseEntity<List<SavedReportResponse>> getSavedReports(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.ok(reportService.getSavedReports(actor));
    }

    @DeleteMapping("/saved/{id}")
    @Operation(summary = "Delete saved report", description = "Deletes a saved report configuration")
    public ResponseEntity<Void> deleteSavedReport(
            @Parameter(description = "Report ID") @PathVariable UUID id) {
        reportService.deleteSavedReport(id);
        return ResponseEntity.noContent().build();
    }
}