package com.jira.version.controller;

import com.jira.version.dto.*;
import com.jira.version.entity.ReleaseTrain;
import com.jira.version.entity.VersionAuditLog;
import com.jira.version.entity.VersionDeployment;
import com.jira.version.entity.VersionBuildReference;
import com.jira.version.service.VersionService;
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
@RequestMapping("/api/versions")
@RequiredArgsConstructor
@Tag(name = "Version Management", description = "Enterprise-grade version management API")
public class VersionController {

    private final VersionService versionService;

    // ========== VERSION CRUD ==========

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all versions for a project")
    public ResponseEntity<List<VersionResponse>> getVersionsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(versionService.getVersionsByProject(projectId, includeArchived));
    }

    @GetMapping("/{versionId}")
    @Operation(summary = "Get version by ID")
    public ResponseEntity<VersionResponse> getVersionById(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.getVersionById(versionId));
    }

    @PostMapping
    @Operation(summary = "Create a new version")
    public ResponseEntity<VersionResponse> createVersion(@Valid @RequestBody CreateVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.createVersion(request));
    }

    @PutMapping("/{versionId}")
    @Operation(summary = "Update a version")
    public ResponseEntity<VersionResponse> updateVersion(
            @PathVariable UUID versionId,
            @Valid @RequestBody UpdateVersionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(versionService.updateVersion(versionId, request, userId));
    }

    @DeleteMapping("/{versionId}")
    @Operation(summary = "Delete a version")
    public ResponseEntity<Void> deleteVersion(@PathVariable UUID versionId) {
        versionService.deleteVersion(versionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{versionId}/restore")
    @Operation(summary = "Restore a deleted version")
    public ResponseEntity<VersionResponse> restoreVersion(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.restoreVersion(versionId));
    }

    // ========== RELEASE OPERATIONS ==========

    @PostMapping("/{versionId}/release")
    @Operation(summary = "Release a version")
    public ResponseEntity<VersionResponse> releaseVersion(
            @PathVariable UUID versionId,
            @RequestBody ReleaseVersionRequest request) {
        return ResponseEntity.ok(versionService.releaseVersion(versionId, request));
    }

    @PostMapping("/{versionId}/archive")
    @Operation(summary = "Archive a version")
    public ResponseEntity<VersionResponse> archiveVersion(
            @PathVariable UUID versionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(versionService.archiveVersion(versionId, userId));
    }

    @PostMapping("/{versionId}/unarchive")
    @Operation(summary = "Unarchive a version")
    public ResponseEntity<VersionResponse> unarchiveVersion(
            @PathVariable UUID versionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(versionService.unarchiveVersion(versionId, userId));
    }

    // ========== ISSUE VERSION LINKING ==========

    @PostMapping("/fix-version")
    @Operation(summary = "Assign fix version to an issue")
    public ResponseEntity<Void> assignFixVersion(
            @RequestParam UUID issueId,
            @RequestParam UUID versionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        versionService.assignFixVersion(issueId, versionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/fix-version")
    @Operation(summary = "Remove fix version from an issue")
    public ResponseEntity<Void> removeFixVersion(
            @RequestParam UUID issueId,
            @RequestParam UUID versionId) {
        versionService.removeFixVersion(issueId, versionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/affects-version")
    @Operation(summary = "Assign affects version to an issue")
    public ResponseEntity<Void> assignAffectsVersion(
            @RequestParam UUID issueId,
            @RequestParam UUID versionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        versionService.assignAffectsVersion(issueId, versionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/affects-version")
    @Operation(summary = "Remove affects version from an issue")
    public ResponseEntity<Void> removeAffectsVersion(
            @RequestParam UUID issueId,
            @RequestParam UUID versionId) {
        versionService.removeAffectsVersion(issueId, versionId);
        return ResponseEntity.noContent().build();
    }

    // ========== BULK OPERATIONS ==========

    @PostMapping("/bulk-assign")
    @Operation(summary = "Bulk assign issues to a version")
    public ResponseEntity<Integer> bulkAssignFixVersion(@RequestBody BulkAssignVersionRequest request) {
        int count = versionService.bulkAssignFixVersion(request.getIssueIds(), request.getVersionId(), null);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/bulk-move")
    @Operation(summary = "Bulk move issues between versions")
    public ResponseEntity<Integer> bulkMoveFixVersion(@RequestBody BulkAssignVersionRequest request) {
        int count = versionService.bulkMoveFixVersion(
            request.getIssueIds(),
            request.getVersionId(), // source
            request.getTargetVersionId(), // target
            null
        );
        return ResponseEntity.ok(count);
    }

    // ========== MERGE ==========

    @PostMapping("/merge")
    @Operation(summary = "Merge two versions")
    public ResponseEntity<VersionResponse> mergeVersions(@Valid @RequestBody MergeVersionsRequest request) {
        return ResponseEntity.ok(versionService.mergeVersions(request));
    }

    // ========== RELEASE NOTES ==========

    @PostMapping("/{versionId}/release-notes/generate")
    @Operation(summary = "Generate release notes for a version")
    public ResponseEntity<VersionReleaseNoteResponse> generateReleaseNotes(
            @PathVariable UUID versionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(versionService.generateReleaseNotes(versionId, userId));
    }

    @GetMapping("/{versionId}/release-notes")
    @Operation(summary = "Get release notes for a version")
    public ResponseEntity<VersionReleaseNoteResponse> getReleaseNotes(@PathVariable UUID versionId) {
        // This would need a separate method in service
        return ResponseEntity.ok(null);
    }

    // ========== METRICS ==========

    @GetMapping("/{versionId}/metrics")
    @Operation(summary = "Get version metrics history")
    public ResponseEntity<List<VersionMetricsResponse>> getVersionMetrics(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.getVersionMetrics(versionId));
    }

    @PostMapping("/{versionId}/metrics/snapshot")
    @Operation(summary = "Record a metrics snapshot")
    public ResponseEntity<VersionMetricsResponse> recordMetricsSnapshot(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.recordMetricsSnapshot(versionId));
    }

    // ========== DEPLOYMENTS ==========

    @GetMapping("/{versionId}/deployments")
    @Operation(summary = "Get version deployments")
    public ResponseEntity<List<VersionDeploymentResponse>> getVersionDeployments(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.getVersionDeployments(versionId));
    }

    @PostMapping("/{versionId}/deployments")
    @Operation(summary = "Add deployment to version")
    public ResponseEntity<VersionDeploymentResponse> addDeployment(
            @PathVariable UUID versionId,
            @RequestBody VersionDeployment deployment) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(versionService.addDeployment(versionId, deployment));
    }

    // ========== BUILDS ==========

    @GetMapping("/{versionId}/builds")
    @Operation(summary = "Get version build references")
    public ResponseEntity<List<VersionBuildReferenceResponse>> getVersionBuilds(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.getVersionBuilds(versionId));
    }

    @PostMapping("/{versionId}/builds")
    @Operation(summary = "Add build reference to version")
    public ResponseEntity<VersionBuildReferenceResponse> addBuildReference(
            @PathVariable UUID versionId,
            @RequestBody VersionBuildReference build) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(versionService.addBuildReference(versionId, build));
    }

    // ========== RELEASE TRAINS ==========

    @GetMapping("/trains")
    @Operation(summary = "Get all release trains")
    public ResponseEntity<List<ReleaseTrain>> getReleaseTrains() {
        return ResponseEntity.ok(versionService.getReleaseTrains());
    }

    @PostMapping("/trains")
    @Operation(summary = "Create a release train")
    public ResponseEntity<ReleaseTrain> createReleaseTrain(@RequestBody ReleaseTrain train) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.createReleaseTrain(train));
    }

    @PostMapping("/trains/{trainId}/versions/{versionId}")
    @Operation(summary = "Add version to release train")
    public ResponseEntity<Void> addVersionToReleaseTrain(
            @PathVariable UUID trainId,
            @PathVariable UUID versionId) {
        versionService.addVersionToReleaseTrain(trainId, versionId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/trains/{trainId}/versions/{versionId}")
    @Operation(summary = "Remove version from release train")
    public ResponseEntity<Void> removeVersionFromReleaseTrain(
            @PathVariable UUID trainId,
            @PathVariable UUID versionId) {
        versionService.removeVersionFromReleaseTrain(trainId, versionId);
        return ResponseEntity.noContent().build();
    }

    // ========== AUDIT ==========

    @GetMapping("/{versionId}/audit")
    @Operation(summary = "Get version audit logs")
    public ResponseEntity<List<VersionAuditLog>> getVersionAuditLogs(@PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.getVersionAuditLogs(versionId));
    }
}