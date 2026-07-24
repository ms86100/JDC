package com.jira.test.controller;

import com.jira.test.dto.BenchDefectResponse;
import com.jira.test.dto.CreateBenchDefectRequest;
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
@RequestMapping("/api/bench-defects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bench Defect Management", description = "Test means anomaly tracking")
public class BenchDefectController {

    private final DefectManagementService defectManagementService;

    @PostMapping
    @Operation(summary = "Create a new Bench Defect")
    public ResponseEntity<BenchDefectResponse> create(@Valid @RequestBody CreateBenchDefectRequest request) {
        log.info("POST /api/bench-defects - Creating BenchDefect for project: {}", request.getProjectId());
        BenchDefectResponse response = defectManagementService.createBenchDefect(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a Bench Defect by ID")
    public ResponseEntity<BenchDefectResponse> getById(@PathVariable UUID id) {
        log.info("GET /api/bench-defects/{}", id);
        BenchDefectResponse response = defectManagementService.getBenchDefect(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List Bench Defects by project")
    public ResponseEntity<List<BenchDefectResponse>> getByProject(@PathVariable UUID projectId) {
        log.info("GET /api/bench-defects/project/{}", projectId);
        List<BenchDefectResponse> responses = defectManagementService.getBenchDefectsByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a Bench Defect")
    public ResponseEntity<BenchDefectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateBenchDefectRequest request) {
        log.info("PUT /api/bench-defects/{}", id);
        BenchDefectResponse response = defectManagementService.updateBenchDefect(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-tech-event/{techEventId}")
    @Operation(summary = "List Bench Defects by source Tech Event")
    public ResponseEntity<List<BenchDefectResponse>> getByTechEvent(@PathVariable UUID techEventId) {
        log.info("GET /api/bench-defects/by-tech-event/{}", techEventId);
        List<BenchDefectResponse> responses = defectManagementService.getBenchDefectsByTechEvent(techEventId);
        return ResponseEntity.ok(responses);
    }
}
