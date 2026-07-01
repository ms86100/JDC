package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.TraceabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Traceability", description = "APIs for requirement-test traceability management")
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    @PostMapping("/requirements/links")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Link a requirement to a test")
    public ResponseEntity<RequirementLinkResponse> linkRequirement(@Valid @RequestBody RequirementLinkRequest request) {
        RequirementLinkResponse link = traceabilityService.linkRequirementToTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @GetMapping("/traceability/matrix")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get the full traceability matrix for a project")
    public ResponseEntity<TraceabilityMatrixResponse> getTraceabilityMatrix(@RequestParam UUID projectId) {
        TraceabilityMatrixResponse matrix = traceabilityService.getTraceabilityMatrix(projectId);
        return ResponseEntity.ok(matrix);
    }

    @GetMapping("/traceability/coverage")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get test coverage for a requirement")
    public ResponseEntity<List<TestResponse>> getCoverageForRequirement(
            @RequestParam String requirementKey,
            @RequestParam UUID projectId) {
        List<TestResponse> tests = traceabilityService.getCoverageForRequirement(requirementKey);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/traceability/defects")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get defects linked to a test")
    public ResponseEntity<List<DefectLinkResponse>> getDefectsForTest(@RequestParam UUID testId, @RequestParam UUID projectId) {
        List<DefectLinkResponse> defects = traceabilityService.getDefectsForTest(testId);
        return ResponseEntity.ok(defects);
    }

    @PostMapping("/defects/links")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Link a defect to a test execution")
    public ResponseEntity<DefectLinkResponse> linkDefect(
            @RequestParam UUID projectId,
            @Valid @RequestBody DefectLinkRequest request) {
        DefectLinkResponse defectLink = traceabilityService.linkDefect(
                request.getExecutionId(), request.getStepResultId(), request.getDefectKey(), request.getSeverity());
        return ResponseEntity.status(HttpStatus.CREATED).body(defectLink);
    }
}