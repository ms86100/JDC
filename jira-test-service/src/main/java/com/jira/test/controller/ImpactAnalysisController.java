package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.ImpactAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/impact")
@RequiredArgsConstructor
@Tag(name = "Test Impact Analysis", description = "APIs for analyzing test impact based on code changes")
public class ImpactAnalysisController {

    private final ImpactAnalysisService impactAnalysisService;

    // ==================== Impact Analysis ====================

    @PostMapping("/analyze")
    @Operation(summary = "Analyze impact of code changes on tests")
    public ResponseEntity<ImpactAnalysisResponse> analyzeImpact(@Valid @RequestBody ImpactAnalysisRequest request) {
        ImpactAnalysisResponse result = impactAnalysisService.analyzeImpact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/analyze/commit")
    @Operation(summary = "Analyze impact of a specific commit")
    public ResponseEntity<ImpactAnalysisResponse> analyzeCommit(@Valid @RequestBody ImpactAnalysisRequest request) {
        request.setTriggerType("commit");
        ImpactAnalysisResponse result = impactAnalysisService.analyzeImpact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/analyze/pr")
    @Operation(summary = "Analyze impact of a pull request")
    public ResponseEntity<ImpactAnalysisResponse> analyzePullRequest(@Valid @RequestBody ImpactAnalysisRequest request) {
        request.setTriggerType("pr");
        ImpactAnalysisResponse result = impactAnalysisService.analyzeImpact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/results/{analysisId}")
    @Operation(summary = "Get a specific analysis result")
    public ResponseEntity<ImpactAnalysisResponse> getAnalysisResult(@PathVariable UUID analysisId) {
        ImpactAnalysisResponse result = impactAnalysisService.getAnalysisResult(analysisId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history/{projectId}")
    @Operation(summary = "Get analysis history for a project")
    public ResponseEntity<List<ImpactAnalysisResponse>> getAnalysisHistory(@PathVariable UUID projectId) {
        List<ImpactAnalysisResponse> history = impactAnalysisService.getAnalysisHistory(projectId);
        return ResponseEntity.ok(history);
    }

    // ==================== Component Management ====================

    @PostMapping("/components")
    @Operation(summary = "Register a new component")
    public ResponseEntity<ComponentResponse> registerComponent(@Valid @RequestBody ComponentRequest request) {
        ComponentResponse component = impactAnalysisService.registerComponent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(component);
    }

    @GetMapping("/components/{componentId}")
    @Operation(summary = "Get component by ID")
    public ResponseEntity<ComponentResponse> getComponent(@PathVariable UUID componentId) {
        ComponentResponse component = impactAnalysisService.getComponent(componentId);
        return ResponseEntity.ok(component);
    }

    @GetMapping("/components/project/{projectId}")
    @Operation(summary = "Get all components for a project")
    public ResponseEntity<List<ComponentResponse>> getComponentsByProject(@PathVariable UUID projectId) {
        List<ComponentResponse> components = impactAnalysisService.getComponentsByProject(projectId);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/components/search")
    @Operation(summary = "Search components by name")
    public ResponseEntity<List<ComponentResponse>> searchComponents(
            @RequestParam UUID projectId,
            @RequestParam String search) {
        List<ComponentResponse> components = impactAnalysisService.searchComponents(projectId, search);
        return ResponseEntity.ok(components);
    }

    @PutMapping("/components/{componentId}")
    @Operation(summary = "Update a component")
    public ResponseEntity<ComponentResponse> updateComponent(
            @PathVariable UUID componentId,
            @Valid @RequestBody ComponentRequest request) {
        ComponentResponse component = impactAnalysisService.updateComponent(componentId, request);
        return ResponseEntity.ok(component);
    }

    @DeleteMapping("/components/{componentId}")
    @Operation(summary = "Delete a component")
    public ResponseEntity<Void> deleteComponent(@PathVariable UUID componentId) {
        impactAnalysisService.deleteComponent(componentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Test-Component Mapping ====================

    @PostMapping("/test-component")
    @Operation(summary = "Map a test to a component")
    public ResponseEntity<Void> mapTestToComponent(@Valid @RequestBody TestComponentMappingRequest request) {
        impactAnalysisService.mapTestToComponent(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test/{testId}/components")
    @Operation(summary = "Get all components mapped to a test")
    public ResponseEntity<List<ComponentResponse>> getComponentsForTest(@PathVariable UUID testId) {
        List<ComponentResponse> components = impactAnalysisService.getComponentsForTest(testId);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/component/{componentId}/tests")
    @Operation(summary = "Get all tests mapped to a component")
    public ResponseEntity<List<ImpactAnalysisResponse.TestImpactDto>> getTestsForComponent(@PathVariable UUID componentId) {
        List<ImpactAnalysisResponse.TestImpactDto> tests = impactAnalysisService.getTestsForComponent(componentId);
        return ResponseEntity.ok(tests);
    }
}