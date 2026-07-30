package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.SharedStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shared-steps")
@RequiredArgsConstructor
@Tag(name = "Shared Step Library", description = "APIs for managing reusable test steps")
public class SharedStepController {

    private final SharedStepService sharedStepService;

    // ==================== Shared Step CRUD ====================

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new shared step")
    public ResponseEntity<SharedStepResponse> createSharedStep(@Valid @RequestBody CreateSharedStepRequest request) {
        SharedStepResponse sharedStep = sharedStepService.createSharedStep(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sharedStep);
    }

    @GetMapping("/{sharedStepId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a shared step by ID")
    public ResponseEntity<SharedStepResponse> getSharedStep(@PathVariable UUID sharedStepId, @RequestParam UUID projectId) {
        SharedStepResponse sharedStep = sharedStepService.getSharedStep(sharedStepId);
        return ResponseEntity.ok(sharedStep);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all shared steps for a project")
    public ResponseEntity<List<SharedStepResponse>> getSharedStepsByProject(@PathVariable UUID projectId) {
        List<SharedStepResponse> sharedSteps = sharedStepService.getSharedStepsByProject(projectId);
        return ResponseEntity.ok(sharedSteps);
    }

    @GetMapping("/search")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Search shared steps by name or description")
    public ResponseEntity<List<SharedStepResponse>> searchSharedSteps(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String search) {
        List<SharedStepResponse> sharedSteps = sharedStepService.searchSharedSteps(projectId, search);
        return ResponseEntity.ok(sharedSteps);
    }

    @PutMapping("/{sharedStepId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Update a shared step")
    public ResponseEntity<SharedStepResponse> updateSharedStep(
            @PathVariable UUID sharedStepId,
            @RequestParam UUID projectId,
            @Valid @RequestBody CreateSharedStepRequest request) {
        SharedStepResponse sharedStep = sharedStepService.updateSharedStep(sharedStepId, request);
        return ResponseEntity.ok(sharedStep);
    }

    @DeleteMapping("/{sharedStepId}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Archive a shared step")
    public ResponseEntity<Void> deleteSharedStep(@PathVariable UUID sharedStepId, @RequestParam UUID projectId) {
        sharedStepService.deleteSharedStep(sharedStepId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Versioning ====================

    @GetMapping("/{sharedStepId}/versions")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get version history")
    public ResponseEntity<List<SharedStepVersionResponse>> getVersionHistory(
            @PathVariable UUID sharedStepId,
            @RequestParam UUID projectId) {
        List<SharedStepVersionResponse> versions = sharedStepService.getVersionHistory(sharedStepId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{sharedStepId}/versions/{version}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a specific version")
    public ResponseEntity<SharedStepVersionResponse> getVersion(
            @PathVariable UUID sharedStepId,
            @PathVariable Integer version,
            @RequestParam UUID projectId) {
        SharedStepVersionResponse versionResponse = sharedStepService.getVersion(sharedStepId, version);
        return ResponseEntity.ok(versionResponse);
    }

    @PostMapping("/{sharedStepId}/versions")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Create a new version")
    public ResponseEntity<SharedStepVersionResponse> createNewVersion(
            @PathVariable UUID sharedStepId,
            @RequestParam UUID projectId,
            @RequestBody List<SharedStepDto> steps,
            @RequestParam(required = false) String changeSummary) {
        SharedStepVersionResponse version = sharedStepService.createNewVersion(sharedStepId, steps, changeSummary);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    // ==================== Impact Analysis ====================

    @GetMapping("/{sharedStepId}/impact")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get impact analysis - which tests use this shared step")
    public ResponseEntity<List<SharedStepImpactResponse>> getImpactAnalysis(
            @PathVariable UUID sharedStepId,
            @RequestParam UUID projectId) {
        List<SharedStepImpactResponse> impact = sharedStepService.getImpactAnalysis(sharedStepId);
        return ResponseEntity.ok(impact);
    }

    @GetMapping("/{sharedStepId}/affected-tests")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get IDs of tests affected by this shared step")
    public ResponseEntity<List<UUID>> getAffectedTestIds(@PathVariable UUID sharedStepId, @RequestParam UUID projectId) {
        List<UUID> testIds = sharedStepService.getAffectedTestIds(sharedStepId);
        return ResponseEntity.ok(testIds);
    }

    // ==================== Dependencies ====================

    @GetMapping("/{sharedStepId}/dependencies")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get dependency tree")
    public ResponseEntity<List<SharedStepDependencyResponse>> getDependencyTree(
            @PathVariable UUID sharedStepId,
            @RequestParam UUID projectId) {
        List<SharedStepDependencyResponse> dependencies = sharedStepService.getDependencyTree(sharedStepId);
        return ResponseEntity.ok(dependencies);
    }

    @PostMapping("/{sharedStepId}/validate-dependencies")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Validate no circular dependencies before update")
    public ResponseEntity<Map<String, Object>> validateDependencies(
            @PathVariable UUID sharedStepId,
            @RequestParam UUID projectId,
            @RequestBody List<SharedStepDto> newSteps) {
        sharedStepService.validateNoCircularDependencies(sharedStepId, newSteps);
        return ResponseEntity.ok(Map.of("valid", true, "message", "No circular dependencies detected"));
    }

    // ==================== Test Integration ====================

    @GetMapping("/test/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get embedded steps in a test")
    public ResponseEntity<List<EmbeddedStepResponse>> getEmbeddedSteps(@PathVariable UUID testId, @RequestParam UUID projectId) {
        List<EmbeddedStepResponse> embeddedSteps = sharedStepService.getEmbeddedSteps(testId);
        return ResponseEntity.ok(embeddedSteps);
    }

    @PostMapping("/insert")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #request.projectId)")
    @Operation(summary = "Insert a shared step into a test")
    public ResponseEntity<EmbeddedStepResponse> insertSharedStep(@Valid @RequestBody InsertSharedStepRequest request) {
        EmbeddedStepResponse result = sharedStepService.insertSharedStep(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/mapping/{mappingId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Remove shared step from test")
    public ResponseEntity<Void> removeSharedStep(@PathVariable UUID mappingId, @RequestParam UUID projectId) {
        sharedStepService.removeSharedStep(mappingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test/{testId}/snapshot")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #projectId)")
    @Operation(summary = "Get frozen snapshot of all shared steps for execution")
    public ResponseEntity<String> getSnapshotForExecution(
            @PathVariable UUID testId,
            @RequestParam UUID executionId,
            @RequestParam UUID projectId) {
        String snapshot = sharedStepService.snapshotForExecution(testId, executionId);
        return ResponseEntity.ok(snapshot);
    }
}