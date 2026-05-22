package com.jira.test.controller;

import com.jira.test.dto.DriftAnalysisResponse;
import com.jira.test.dto.VersionStatisticsResponse;
import com.jira.test.entity.CoverageDriftRecord;
import com.jira.test.entity.RequirementVersion;
import com.jira.test.service.CoverageDriftService;
import com.jira.test.service.RequirementVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
@Tag(name = "Requirement Version & Drift", description = "APIs for requirement versioning and coverage drift analysis")
public class RequirementController {

    private final RequirementVersionService versionService;
    private final CoverageDriftService driftService;

    // ==================== Version Management ====================

    @PostMapping("/{id}/versions")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Create a new version for a requirement")
    public ResponseEntity<Map<String, Object>> createVersion(
            @Parameter(description = "Requirement ID") @PathVariable UUID id,
            @Parameter(description = "Project ID") @RequestParam UUID projectId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails user) {

        String content = request.get("content");
        String changelog = request.get("changelog");
        UUID changedBy = getUserId(user);

        RequirementVersion version = versionService.createVersion(id, content, changelog, changedBy);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "versionId", version.getId(),
                        "version", version.getVersion(),
                        "versionNumber", version.getVersionNumber(),
                        "status", version.getStatus(),
                        "changeMagnitude", version.getChangeMagnitude(),
                        "createdAt", version.getCreatedAt()
                ));
    }

    @PutMapping("/{id}/versions/{versionId}/publish")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Publish a requirement version")
    public ResponseEntity<Map<String, Object>> publishVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId,
            @RequestParam UUID projectId,
            @AuthenticationPrincipal UserDetails user) {

        UUID publishedBy = getUserId(user);
        RequirementVersion version = versionService.publishVersion(versionId, publishedBy);

        return ResponseEntity.ok(Map.of(
                "versionId", version.getId(),
                "version", version.getVersion(),
                "versionNumber", version.getVersionNumber(),
                "status", version.getStatus(),
                "publishedAt", version.getPublishedAt(),
                "publishedBy", version.getPublishedBy()
        ));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get version history for a requirement")
    public ResponseEntity<List<Map<String, Object>>> getVersionHistory(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        List<RequirementVersion> versions = versionService.getVersions(id, page, size);
        List<Map<String, Object>> history = versions.stream()
                .map(this::formatVersionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/versions/stats")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get version statistics for a requirement")
    public ResponseEntity<Map<String, Object>> getVersionStats(
            @PathVariable UUID id,
            @RequestParam UUID projectId) {

        Map<String, Object> stats = versionService.getVersionStats(id);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/versions/{versionId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a specific version")
    public ResponseEntity<Map<String, Object>> getVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId,
            @RequestParam UUID projectId) {

        return versionService.getVersion(versionId)
                .map(v -> ResponseEntity.ok(formatVersionResponse(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/versions/compare")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Compare two versions")
    public ResponseEntity<Map<String, Object>> compareVersions(
            @PathVariable UUID id,
            @RequestParam UUID versionId1,
            @RequestParam UUID versionId2,
            @RequestParam UUID projectId) {

        Map<String, Object> comparison = versionService.compareVersions(versionId1, versionId2);
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/{id}/versions/{versionId}/diff")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get diff from previous version")
    public ResponseEntity<Map<String, Object>> getDiffFromPrevious(
            @PathVariable UUID id,
            @PathVariable UUID versionId,
            @RequestParam UUID projectId) {

        Map<String, Object> diff = versionService.getDiffFromPrevious(versionId);
        if (diff.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(diff);
    }

    @PostMapping("/{id}/versions/{versionId}/rollback")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Rollback to a specific version")
    public ResponseEntity<Map<String, Object>> rollbackToVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId,
            @RequestParam UUID projectId,
            @AuthenticationPrincipal UserDetails user) {

        UUID changedBy = getUserId(user);
        RequirementVersion newVersion = versionService.rollbackToVersion(id, versionId, changedBy);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Rolled back successfully",
                        "newVersionId", newVersion.getId(),
                        "version", newVersion.getVersion(),
                        "versionNumber", newVersion.getVersionNumber(),
                        "status", newVersion.getStatus()
                ));
    }

    @PutMapping("/{id}/versions/{versionId}/archive")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Archive a version")
    public ResponseEntity<Map<String, Object>> archiveVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId,
            @RequestParam UUID projectId) {

        RequirementVersion version = versionService.archiveVersion(versionId);
        return ResponseEntity.ok(Map.of(
                "versionId", version.getId(),
                "version", version.getVersion(),
                "status", version.getStatus()
        ));
    }

    @GetMapping("/{id}/versions/current")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get the current published version")
    public ResponseEntity<Map<String, Object>> getCurrentVersion(
            @PathVariable UUID id,
            @RequestParam UUID projectId) {

        return versionService.getCurrentVersion(id)
                .map(v -> ResponseEntity.ok(formatVersionResponse(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Coverage Drift ====================

    @GetMapping("/{id}/coverage-drift")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get drift analysis for a requirement")
    public ResponseEntity<Map<String, Object>> getCoverageDrift(
            @PathVariable UUID id,
            @RequestParam UUID projectId) {

        CoverageDriftRecord drift = driftService.recordDrift(id);
        return ResponseEntity.ok(formatDriftResponse(drift));
    }

    @PostMapping("/{id}/coverage-drift/detect")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Force drift detection for a requirement")
    public ResponseEntity<Map<String, Object>> forceDriftDetection(
            @PathVariable UUID id,
            @RequestParam UUID projectId) {

        CoverageDriftRecord drift = driftService.detectDrift(id, projectId);
        return ResponseEntity.ok(formatDriftResponse(drift));
    }

    @GetMapping("/{id}/coverage-trend")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get coverage trend for a requirement")
    public ResponseEntity<Map<String, Object>> getCoverageTrend(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        Map<String, Object> trend = driftService.getCoverageTrend(id, days);
        return ResponseEntity.ok(trend);
    }

    @GetMapping("/{id}/remediation")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get auto-remediation suggestions")
    public ResponseEntity<Map<String, Object>> getRemediation(
            @PathVariable UUID id,
            @RequestParam UUID projectId) {

        Map<String, Object> remediation = driftService.autoRemediate(id);
        return ResponseEntity.ok(remediation);
    }

    @PostMapping("/{id}/remediation/apply")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Apply remediation suggestions")
    public ResponseEntity<Map<String, Object>> applyRemediation(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestBody Map<String, List<String>> request) {

        List<String> suggestionTypes = request.getOrDefault("types", List.of("ADD_TEST", "UPDATE_TEST"));
        Map<String, Object> results = driftService.applyRemediation(id, suggestionTypes);
        return ResponseEntity.ok(results);
    }

    // ==================== Project-Level Drift Operations ====================

    @PostMapping("/coverage-drift/detect-all")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #projectId)")
    @Operation(summary = "Detect all drifts for a project")
    public ResponseEntity<Map<String, Object>> detectAllDrifts(
            @RequestParam UUID projectId) {

        List<CoverageDriftRecord> drifts = driftService.detectAllDrifts(projectId);

        long improvedCount = drifts.stream()
                .filter(d -> d.getDriftType() == CoverageDriftRecord.DriftType.IMPROVED)
                .count();
        long degradedCount = drifts.stream()
                .filter(d -> d.getDriftType() == CoverageDriftRecord.DriftType.DEGRADED)
                .count();
        long actionRequiredCount = drifts.stream()
                .filter(d -> Boolean.TRUE.equals(d.getActionRequired()))
                .count();

        return ResponseEntity.ok(Map.of(
                "totalDetected", drifts.size(),
                "improved", improvedCount,
                "degraded", degradedCount,
                "actionRequired", actionRequiredCount,
                "drifts", drifts.stream().map(this::formatDriftResponse).collect(Collectors.toList())
        ));
    }

    @GetMapping("/coverage-drift/summary")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get drift summary for a project")
    public ResponseEntity<Map<String, Object>> getDriftSummary(
            @RequestParam UUID projectId) {

        Map<String, Object> summary = driftService.getDriftSummary(projectId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/coverage-drift/alerts")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get drift alerts for a project")
    public ResponseEntity<List<Map<String, Object>>> getDriftAlerts(
            @RequestParam UUID projectId,
            @RequestParam(required = false) BigDecimal threshold) {

        List<Map<String, Object>> alerts = driftService.getDriftAlerts(projectId, threshold);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/coverage-drift/history")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get drift history for a project")
    public ResponseEntity<List<Map<String, Object>>> getDriftHistory(
            @RequestParam UUID projectId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        List<Map<String, Object>> history = driftService.getDriftHistory(projectId, days);
        return ResponseEntity.ok(history);
    }

    // ==================== Helper Methods ====================

    private UUID getUserId(UserDetails user) {
        if (user == null) return null;
        return UUID.nameUUIDFromBytes(user.getUsername().getBytes());
    }

    private Map<String, Object> formatVersionResponse(RequirementVersion version) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", version.getId());
        response.put("requirementId", version.getRequirementId());
        response.put("version", version.getVersion());
        response.put("versionNumber", version.getVersionNumber() != null ? version.getVersionNumber() : 0);
        response.put("status", version.getStatus());
        response.put("changeMagnitude", version.getChangeMagnitude());
        response.put("content", version.getContent() != null ? version.getContent().substring(0, Math.min(200, version.getContent().length())) : "");
        response.put("contentLength", version.getContent() != null ? version.getContent().length() : 0);
        response.put("changelog", version.getChangelog() != null ? version.getChangelog() : "");
        response.put("createdAt", version.getCreatedAt());
        response.put("publishedAt", version.getPublishedAt());
        response.put("publishedBy", version.getPublishedBy());
        response.put("titleSnapshot", version.getTitleSnapshot() != null ? version.getTitleSnapshot() : "");
        response.put("previousVersionId", version.getPreviousVersionId());
        return response;
    }

    private Map<String, Object> formatDriftResponse(CoverageDriftRecord drift) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", drift.getId());
        response.put("requirementId", drift.getRequirementId());
        response.put("projectId", drift.getProjectId());
        response.put("previousCoverage", drift.getPreviousCoverage());
        response.put("currentCoverage", drift.getCurrentCoverage());
        response.put("drift", drift.getDrift());
        response.put("driftType", drift.getDriftType());
        response.put("previousTestCount", drift.getPreviousTestCount());
        response.put("currentTestCount", drift.getCurrentTestCount());
        response.put("actionRequired", drift.getActionRequired());
        response.put("detectedAt", drift.getDetectedAt());

        // Parse JSON fields for better formatting
        try {
            if (drift.getAffectedTests() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> tests = mapper.readValue(drift.getAffectedTests(), List.class);
                response.put("affectedTestCount", tests.size());
                response.put("affectedTests", tests);
            }
            if (drift.getMissingCoverage() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> missing = mapper.readValue(drift.getMissingCoverage(), List.class);
                response.put("missingCoverage", missing);
            }
            if (drift.getStaleCoverage() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> stale = mapper.readValue(drift.getStaleCoverage(), List.class);
                response.put("staleCoverage", stale);
            }
        } catch (Exception e) {
            // JSON parsing failed, include raw data
        }

        return response;
    }
}