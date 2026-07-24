package com.jira.test.controller;

import com.jira.test.dto.CreateProblemReportRequest;
import com.jira.test.dto.ProblemReportResponse;
import com.jira.test.service.DefectManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/problem-reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problem Report Management", description = "Certification-facing problem reports")
public class ProblemReportController {

    private final DefectManagementService defectManagementService;

    @PostMapping
    @Operation(summary = "Create a new Problem Report")
    public ResponseEntity<ProblemReportResponse> create(@Valid @RequestBody CreateProblemReportRequest request) {
        log.info("POST /api/problem-reports - Creating ProblemReport for project: {}", request.getProjectId());
        ProblemReportResponse response = defectManagementService.createProblemReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a Problem Report by ID")
    public ResponseEntity<ProblemReportResponse> getById(@PathVariable UUID id) {
        log.info("GET /api/problem-reports/{}", id);
        ProblemReportResponse response = defectManagementService.getProblemReport(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List Problem Reports by project")
    public ResponseEntity<List<ProblemReportResponse>> getByProject(@PathVariable UUID projectId) {
        log.info("GET /api/problem-reports/project/{}", projectId);
        List<ProblemReportResponse> responses = defectManagementService.getProblemReportsByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a Problem Report")
    public ResponseEntity<ProblemReportResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProblemReportRequest request) {
        log.info("PUT /api/problem-reports/{}", id);
        ProblemReportResponse response = defectManagementService.updateProblemReport(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-tech-event/{techEventId}")
    @Operation(summary = "List Problem Reports by linked Tech Event")
    public ResponseEntity<List<ProblemReportResponse>> getByTechEvent(@PathVariable UUID techEventId) {
        log.info("GET /api/problem-reports/by-tech-event/{}", techEventId);
        List<ProblemReportResponse> responses = defectManagementService.getProblemReportsByTechEvent(techEventId);
        return ResponseEntity.ok(responses);
    }
}
