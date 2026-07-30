package com.avionics_systems.test.controller;

import com.avionics_systems.test.service.RequirementImpactService;
import com.avionics_systems.test.service.TimelineReplayService;
import com.avionics_systems.test.service.VersionDiffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Advanced Test Features", description = "APIs for requirement impact, timeline replay, and version diff")
public class AdvancedTestController {

    private final RequirementImpactService requirementImpactService;
    private final TimelineReplayService timelineReplayService;
    private final VersionDiffService versionDiffService;

    // ==================== Requirement Impact ====================

    @PostMapping("/requirements/{requirementId}/versions")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #projectId)")
    @Operation(summary = "Create version snapshot for requirement")
    public ResponseEntity<Void> createRequirementVersion(
            @PathVariable UUID requirementId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam UUID projectId) {
        requirementImpactService.createVersionSnapshot(requirementId, title, description, List.of(), null);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/requirements/{requirementId}/versions")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get requirement version history")
    public ResponseEntity<List<Map<String, Object>>> getRequirementVersions(
            @PathVariable UUID requirementId,
            @RequestParam UUID projectId) {
        List<Map<String, Object>> versions = requirementImpactService.getVersionHistory(requirementId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/requirements/{requirementId}/impact")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Analyze requirement change impact")
    public ResponseEntity<Map<String, Object>> analyzeRequirementImpact(
            @PathVariable UUID requirementId,
            @RequestParam Integer fromVersion,
            @RequestParam Integer toVersion,
            @RequestParam UUID projectId) {
        Map<String, Object> impact = requirementImpactService.analyzeChangeImpact(requirementId, fromVersion, toVersion);
        return ResponseEntity.ok(impact);
    }

    @PostMapping("/requirements/{requirementId}/coverage-drift")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Analyze coverage drift")
    public ResponseEntity<Void> analyzeCoverageDrift(@PathVariable UUID requirementId, @RequestParam UUID projectId) {
        requirementImpactService.analyzeCoverageDrift(requirementId);
        return ResponseEntity.ok().build();
    }

    // ==================== Timeline Replay ====================

    @GetMapping("/executions/{executionId}/timeline")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get execution timeline")
    public ResponseEntity<List<Map<String, Object>>> getTimeline(@PathVariable UUID executionId, @RequestParam UUID projectId) {
        List<Map<String, Object>> timeline = timelineReplayService.getTimeline(executionId);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/executions/{executionId}/replay/start")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Start replay session")
    public ResponseEntity<Map<String, Object>> startReplay(@PathVariable UUID executionId, @RequestParam UUID projectId) {
        Map<String, Object> session = timelineReplayService.startReplay(executionId, null, "Replay session");
        return ResponseEntity.ok(session);
    }

    @PutMapping("/replay/{sessionId}/position")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Update playback position")
    public ResponseEntity<Void> updatePlaybackPosition(
            @PathVariable UUID sessionId,
            @RequestParam Integer positionMs,
            @RequestParam UUID projectId) {
        timelineReplayService.updatePlaybackPosition(sessionId, positionMs);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/replay/{sessionId}/pause")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Pause replay")
    public ResponseEntity<Void> pauseReplay(@PathVariable UUID sessionId, @RequestParam UUID projectId) {
        timelineReplayService.pauseReplay(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/replay/{sessionId}/resume")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Resume replay")
    public ResponseEntity<Void> resumeReplay(@PathVariable UUID sessionId, @RequestParam UUID projectId) {
        timelineReplayService.resumeReplay(sessionId);
        return ResponseEntity.ok().build();
    }

    // ==================== Version Diff ====================

    @GetMapping("/tests/{testId}/versions/{v1}/diff/{v2}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Diff two test versions")
    public ResponseEntity<Map<String, Object>> diffTestVersions(
            @PathVariable UUID testId,
            @PathVariable Integer v1,
            @PathVariable Integer v2,
            @RequestParam UUID projectId) {
        Map<String, Object> diff = versionDiffService.diffTestVersions(testId, v1, v2);
        return ResponseEntity.ok(diff);
    }

    @GetMapping("/datasets/{datasetId}/versions/{v1}/diff/{v2}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Diff two dataset versions")
    public ResponseEntity<Map<String, Object>> diffDatasetVersions(
            @PathVariable UUID datasetId,
            @PathVariable Integer v1,
            @PathVariable Integer v2,
            @RequestParam UUID projectId) {
        Map<String, Object> diff = versionDiffService.diffDatasetVersions(datasetId, v1, v2);
        return ResponseEntity.ok(diff);
    }

    @GetMapping("/shared-steps/{sharedStepId}/versions/{v1}/diff/{v2}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Diff two shared step versions")
    public ResponseEntity<Map<String, Object>> diffSharedStepVersions(
            @PathVariable UUID sharedStepId,
            @PathVariable Integer v1,
            @PathVariable Integer v2,
            @RequestParam UUID projectId) {
        Map<String, Object> diff = versionDiffService.diffSharedStepVersions(sharedStepId, v1, v2);
        return ResponseEntity.ok(diff);
    }
}