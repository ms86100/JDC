package com.jira.sprint.controller;

import com.jira.sprint.dto.BurndownResponse;
import com.jira.sprint.dto.SprintReportResponse;
import com.jira.sprint.dto.VelocityResponse;
import com.jira.sprint.service.SprintReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sprints/reports")
@RequiredArgsConstructor
@Tag(name = "Sprint Reports", description = "Sprint reports and analytics API")
@CrossOrigin(origins = "*")
public class SprintReportController {

    private final SprintReportService sprintReportService;

    @GetMapping("/{sprintId}")
    @Operation(summary = "Get sprint report", description = "Get comprehensive sprint report with all metrics")
    public ResponseEntity<SprintReportResponse> getSprintReport(
            @PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintReportService.getSprintReport(sprintId));
    }

    @GetMapping("/{sprintId}/burndown")
    @Operation(summary = "Get burndown chart data", description = "Get burndown chart data for a sprint")
    public ResponseEntity<BurndownResponse> getBurndownChart(
            @PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintReportService.getBurndown(sprintId));
    }

    @GetMapping("/velocity")
    @Operation(summary = "Get velocity metrics", description = "Get velocity metrics for a project")
    public ResponseEntity<VelocityResponse> getVelocityMetrics(
            @RequestParam UUID projectId) {
        return ResponseEntity.ok(sprintReportService.getVelocity(projectId));
    }
}