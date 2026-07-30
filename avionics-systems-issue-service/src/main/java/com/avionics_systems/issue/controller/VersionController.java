package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.CreateVersionRequest;
import com.avionics_systems.issue.dto.UpdateVersionRequest;
import com.avionics_systems.issue.dto.VersionResponse;
import com.avionics_systems.issue.service.VersionService;
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
@RequestMapping("/api/versions")
@RequiredArgsConstructor
@Tag(name = "Versions", description = "Project version management endpoints")
public class VersionController {

    private final VersionService versionService;

    @PostMapping
    @Operation(summary = "Create a new version", description = "Creates a new version in the specified project")
    public ResponseEntity<VersionResponse> createVersion(
            @Valid @RequestBody CreateVersionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        VersionResponse response = versionService.createVersion(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get versions for project", description = "Returns all versions for a project")
    public ResponseEntity<List<VersionResponse>> getVersionsForProject(
            @Parameter(description = "Project ID") @RequestParam UUID projectId,
            @Parameter(description = "Filter by released status") @RequestParam(required = false) Boolean released,
            @Parameter(description = "Filter by archived status") @RequestParam(required = false) Boolean archived) {

        List<VersionResponse> versions;
        if (Boolean.TRUE.equals(released)) {
            versions = versionService.getReleasedVersions(projectId);
        } else if (Boolean.FALSE.equals(released)) {
            versions = versionService.getUnreleasedVersions(projectId);
        } else {
            versions = versionService.getVersionsForProject(projectId);
        }
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get versions for project (path variant)", description = "Returns all versions for a project via path")
    public ResponseEntity<List<VersionResponse>> getVersionsForProjectByPath(
            @Parameter(description = "Project ID") @PathVariable UUID projectId,
            @Parameter(description = "Include archived versions") @RequestParam(required = false) Boolean includeArchived) {

        List<VersionResponse> versions = versionService.getVersionsForProject(projectId);
        if (!Boolean.TRUE.equals(includeArchived)) {
            versions = versions.stream().filter(v -> !Boolean.TRUE.equals(v.getIsArchived())).toList();
        }
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get version by ID", description = "Returns version details by ID")
    public ResponseEntity<VersionResponse> getVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id) {

        VersionResponse response = versionService.getVersion(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update version", description = "Updates version details")
    public ResponseEntity<VersionResponse> updateVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateVersionRequest request) {

        VersionResponse response = versionService.updateVersion(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release version", description = "Marks a version as released")
    public ResponseEntity<VersionResponse> releaseVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {

        VersionResponse response = versionService.releaseVersion(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/unrelease")
    @Operation(summary = "Unrelease version", description = "Marks a version as unreleased")
    public ResponseEntity<VersionResponse> unreleaseVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id) {

        VersionResponse response = versionService.unreleaseVersion(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive version", description = "Archives a version")
    public ResponseEntity<VersionResponse> archiveVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id) {

        VersionResponse response = versionService.archiveVersion(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/unarchive")
    @Operation(summary = "Unarchive version", description = "Unarchives a version")
    public ResponseEntity<VersionResponse> unarchiveVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id) {

        VersionResponse response = versionService.unarchiveVersion(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete version", description = "Deletes a version")
    public ResponseEntity<Void> deleteVersion(
            @Parameter(description = "Version ID") @PathVariable UUID id) {

        versionService.deleteVersion(id);
        return ResponseEntity.noContent().build();
    }
}