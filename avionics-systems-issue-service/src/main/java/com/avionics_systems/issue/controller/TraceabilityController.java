package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.TestManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Traceability Controller
 */
@RestController
@RequestMapping("/api/traceability")
@RequiredArgsConstructor
@Tag(name = "Traceability", description = "Test traceability APIs")
public class TraceabilityController {

    private final TestManagementService testService;

    @PostMapping("/requirements")
    @Operation(summary = "Link a requirement to a test")
    public ResponseEntity<RequirementLinkResponse> linkRequirement(
            @RequestBody RequirementLinkRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.linkRequirement(request, userId));
    }

    @GetMapping("/requirements")
    @Operation(summary = "Get requirement links for a test")
    public ResponseEntity<List<RequirementLinkResponse>> getRequirementLinks(@RequestParam UUID testId) {
        return ResponseEntity.ok(testService.getRequirementLinks(testId));
    }

    @DeleteMapping("/requirements/{linkId}")
    @Operation(summary = "Remove a requirement link")
    public ResponseEntity<Void> removeRequirementLink(@PathVariable UUID linkId) {
        testService.removeRequirementLink(linkId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/defects")
    @Operation(summary = "Link a defect to a test execution")
    public ResponseEntity<DefectLinkResponse> linkDefect(
            @RequestBody DefectLinkRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.linkDefect(request, userId));
    }

    @GetMapping("/defects")
    @Operation(summary = "Get defect links for a test execution")
    public ResponseEntity<List<DefectLinkResponse>> getDefectLinks(@RequestParam UUID executionId) {
        return ResponseEntity.ok(testService.getDefectLinks(executionId));
    }

    @GetMapping("/matrix")
    @Operation(summary = "Get the full traceability matrix")
    public ResponseEntity<TraceabilityMatrixResponse> getTraceabilityMatrix(@RequestParam UUID projectId) {
        return ResponseEntity.ok(testService.getTraceabilityMatrix(projectId));
    }

    @GetMapping("/coverage")
    @Operation(summary = "Get test coverage for a requirement")
    public ResponseEntity<TraceabilityMatrixResponse.RequirementRow> getRequirementCoverage(
            @RequestParam String requirementKey) {
        return ResponseEntity.ok(testService.getRequirementCoverage(requirementKey));
    }
}