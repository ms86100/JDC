package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.ImpactAnalysisService;
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
@RequestMapping("/api/impact")
@RequiredArgsConstructor
@Tag(name = "Test Impact Analysis", description = "APIs for analyzing test impact based on code changes")
public class ImpactAnalysisController {

    private final ImpactAnalysisService impactAnalysisService;

    // ==================== Test Impact Analysis ====================

    @GetMapping("/test/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Analyze impact of a specific test")
    public ResponseEntity<TestImpactDetailDto> analyzeTestImpact(
            @PathVariable UUID testId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false, defaultValue = "3") Integer cascadeDepth) {
        TestImpactDetailDto result = impactAnalysisService.analyzeTestImpact(testId, cascadeDepth);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/requirement/{reqId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Analyze impact of requirement changes")
    public ResponseEntity<RequirementImpactDto> analyzeRequirementImpact(
            @PathVariable String reqId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false, defaultValue = "1") Integer fromVersion,
            @RequestParam(required = false, defaultValue = "2") Integer toVersion) {
        RequirementImpactDto result = impactAnalysisService.analyzeRequirementImpact(reqId, fromVersion, toVersion);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Batch impact analysis for multiple tests")
    public ResponseEntity<BatchImpactAnalysisResponse> batchAnalyze(
            @Valid @RequestBody BatchImpactAnalysisRequest request) {
        BatchImpactAnalysisResponse result = impactAnalysisService.analyzeBatchImpact(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/graph/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get dependency graph for a test")
    public ResponseEntity<List<ImpactGraphDto>> getDependencyGraph(
            @PathVariable UUID testId,
            @RequestParam UUID projectId,
            @RequestParam(required = false) Integer maxDepth) {
        List<ImpactGraphDto> graph = impactAnalysisService.getDependencyGraph(projectId, testId, maxDepth);
        return ResponseEntity.ok(graph);
    }

    @PostMapping("/graph/build/{sourceType}/{sourceId}")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #projectId)")
    @Operation(summary = "Build dependency graph for a source entity")
    public ResponseEntity<ImpactGraphDto> buildGraph(
            @PathVariable String sourceType,
            @PathVariable UUID sourceId,
            @RequestParam UUID projectId) {
        ImpactGraphDto graph = impactAnalysisService.buildDependencyGraph(projectId, sourceType, sourceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(graph);
    }

    @GetMapping("/affected-tests")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get tests affected by changes")
    public ResponseEntity<List<TestImpactDetailDto>> getAffectedTests(
            @RequestParam UUID projectId,
            @RequestParam String changeType, // COMPONENT, REQUIREMENT, FILE
            @RequestParam String changeKey) {
        List<TestImpactDetailDto> affected = impactAnalysisService.getAffectedTests(projectId, changeType, changeKey);
        return ResponseEntity.ok(affected);
    }

    // ==================== Impact Analysis (Legacy/Existing) ====================

    @PostMapping("/analyze")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Analyze impact of code changes on tests")
    public ResponseEntity<ImpactAnalysisResponse> analyzeImpact(@Valid @RequestBody ImpactAnalysisRequest request) {
        ImpactAnalysisResponse result = impactAnalysisService.analyzeImpact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/analyze/commit")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Analyze impact of a specific commit")
    public ResponseEntity<ImpactAnalysisResponse> analyzeCommit(@Valid @RequestBody ImpactAnalysisRequest request) {
        request.setTriggerType("commit");
        ImpactAnalysisResponse result = impactAnalysisService.analyzeImpact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/analyze/pr")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Analyze impact of a pull request")
    public ResponseEntity<ImpactAnalysisResponse> analyzePullRequest(@Valid @RequestBody ImpactAnalysisRequest request) {
        request.setTriggerType("pr");
        ImpactAnalysisResponse result = impactAnalysisService.analyzeImpact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/results/{analysisId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a specific analysis result")
    public ResponseEntity<ImpactAnalysisResponse> getAnalysisResult(@PathVariable UUID analysisId, @RequestParam UUID projectId) {
        ImpactAnalysisResponse result = impactAnalysisService.getAnalysisResult(analysisId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get analysis history for a project")
    public ResponseEntity<List<ImpactAnalysisResponse>> getAnalysisHistory(@PathVariable UUID projectId) {
        List<ImpactAnalysisResponse> history = impactAnalysisService.getAnalysisHistory(projectId);
        return ResponseEntity.ok(history);
    }

    // ==================== Component Management ====================

    @PostMapping("/components")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Register a new component")
    public ResponseEntity<ComponentResponse> registerComponent(@Valid @RequestBody ComponentRequest request) {
        ComponentResponse component = impactAnalysisService.registerComponent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(component);
    }

    @GetMapping("/components/{componentId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get component by ID")
    public ResponseEntity<ComponentResponse> getComponent(@PathVariable UUID componentId, @RequestParam UUID projectId) {
        ComponentResponse component = impactAnalysisService.getComponent(componentId);
        return ResponseEntity.ok(component);
    }

    @GetMapping("/components/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all components for a project")
    public ResponseEntity<List<ComponentResponse>> getComponentsByProject(@PathVariable UUID projectId) {
        List<ComponentResponse> components = impactAnalysisService.getComponentsByProject(projectId);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/components/search")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Search components by name")
    public ResponseEntity<List<ComponentResponse>> searchComponents(
            @RequestParam UUID projectId,
            @RequestParam String search) {
        List<ComponentResponse> components = impactAnalysisService.searchComponents(projectId, search);
        return ResponseEntity.ok(components);
    }

    @PutMapping("/components/{componentId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Update a component")
    public ResponseEntity<ComponentResponse> updateComponent(
            @PathVariable UUID componentId,
            @RequestParam UUID projectId,
            @Valid @RequestBody ComponentRequest request) {
        ComponentResponse component = impactAnalysisService.updateComponent(componentId, request);
        return ResponseEntity.ok(component);
    }

    @DeleteMapping("/components/{componentId}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Delete a component")
    public ResponseEntity<Void> deleteComponent(@PathVariable UUID componentId, @RequestParam UUID projectId) {
        impactAnalysisService.deleteComponent(componentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Test-Component Mapping ====================

    @PostMapping("/test-component")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #request.projectId)")
    @Operation(summary = "Map a test to a component")
    public ResponseEntity<Void> mapTestToComponent(@Valid @RequestBody TestComponentMappingRequest request) {
        impactAnalysisService.mapTestToComponent(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test/{testId}/components")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all components mapped to a test")
    public ResponseEntity<List<ComponentResponse>> getComponentsForTest(@PathVariable UUID testId, @RequestParam UUID projectId) {
        List<ComponentResponse> components = impactAnalysisService.getComponentsForTest(testId);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/component/{componentId}/tests")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all tests mapped to a component")
    public ResponseEntity<List<TestImpactDto>> getTestsForComponent(@PathVariable UUID componentId, @RequestParam UUID projectId) {
        List<TestImpactDto> tests = impactAnalysisService.getTestsForComponent(componentId);
        return ResponseEntity.ok(tests);
    }
}