package com.jira.test.controller;

import com.jira.test.service.RequirementImpactService;
import com.jira.test.service.TimelineReplayService;
import com.jira.test.service.VersionDiffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Create version snapshot for requirement")
    public ResponseEntity<Void> createRequirementVersion(
            @PathVariable UUID requirementId,
            @RequestParam String title,
            @RequestParam(required = false) String description) {
        requirementImpactService.createVersionSnapshot(requirementId, title, description, List.of(), null);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/requirements/{requirementId}/versions")
    @Operation(summary = "Get requirement version history")
    public ResponseEntity<List<Map<String, Object>>> getRequirementVersions(@PathVariable UUID requirementId) {
        List<Map<String, Object>> versions = requirementImpactService.getVersionHistory(requirementId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/requirements/{requirementId}/impact")
    @Operation(summary = "Analyze requirement change impact")
    public ResponseEntity<Map<String, Object>> analyzeRequirementImpact(
            @PathVariable UUID requirementId,
            @RequestParam Integer fromVersion,
            @RequestParam Integer toVersion) {
        Map<String, Object> impact = requirementImpactService.analyzeChangeImpact(requirementId, fromVersion, toVersion);
        return ResponseEntity.ok(impact);
    }

    @PostMapping("/requirements/{requirementId}/coverage-drift")
    @Operation(summary = "Analyze coverage drift")
    public ResponseEntity<Void> analyzeCoverageDrift(@PathVariable UUID requirementId) {
        requirementImpactService.analyzeCoverageDrift(requirementId);
        return ResponseEntity.ok().build();
    }

    // ==================== Timeline Replay ====================

    @GetMapping("/executions/{executionId}/timeline")
    @Operation(summary = "Get execution timeline")
    public ResponseEntity<List<Map<String, Object>>> getTimeline(@PathVariable UUID executionId) {
        List<Map<String, Object>> timeline = timelineReplayService.getTimeline(executionId);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/executions/{executionId}/replay/start")
    @Operation(summary = "Start replay session")
    public ResponseEntity<Map<String, Object>> startReplay(@PathVariable UUID executionId) {
        Map<String, Object> session = timelineReplayService.startReplay(executionId, null);
        return ResponseEntity.ok(session);
    }

    @PutMapping("/replay/{sessionId}/position")
    @Operation(summary = "Update playback position")
    public ResponseEntity<Void> updatePlaybackPosition(
            @PathVariable UUID sessionId,
            @RequestParam Integer positionMs) {
        timelineReplayService.updatePlaybackPosition(sessionId, positionMs);
        return ResponseEntity.ok();
    }

    @PostMapping("/replay/{sessionId}/pause")
    @Operation(summary = "Pause replay")
    public ResponseEntity<Void> pauseReplay(@PathVariable UUID sessionId) {
        timelineReplayService.pauseReplay(sessionId);
        return ResponseEntity.ok();
    }

    @PostMapping("/replay/{sessionId}/resume")
    @Operation(summary = "Resume replay")
    public ResponseEntity<Void> resumeReplay(@PathVariable UUID sessionId) {
        timelineReplayService.resumeReplay(sessionId);
        return ResponseEntity.ok();
    }

    // ==================== Version Diff ====================

    @GetMapping("/tests/{testId}/versions/{v1}/diff/{v2}")
    @Operation(summary = "Diff two test versions")
    public ResponseEntity<Map<String, Object>> diffTestVersions(
            @PathVariable UUID testId,
            @PathVariable Integer v1,
            @PathVariable Integer v2) {
        Map<String, Object> diff = versionDiffService.diffTestVersions(testId, v1, v2);
        return ResponseEntity.ok(diff);
    }

    @GetMapping("/datasets/{datasetId}/versions/{v1}/diff/{v2}")
    @Operation(summary = "Diff two dataset versions")
    public ResponseEntity<Map<String, Object>> diffDatasetVersions(
            @PathVariable UUID datasetId,
            @PathVariable Integer v1,
            @PathVariable Integer v2) {
        Map<String, Object> diff = versionDiffService.diffDatasetVersions(datasetId, v1, v2);
        return ResponseEntity.ok(diff);
    }

    @GetMapping("/shared-steps/{sharedStepId}/versions/{v1}/diff/{v2}")
    @Operation(summary = "Diff two shared step versions")
    public ResponseEntity<Map<String, Object>> diffSharedStepVersions(
            @PathVariable UUID sharedStepId,
            @PathVariable Integer v1,
            @PathVariable Integer v2) {
        Map<String, Object> diff = versionDiffService.diffSharedStepVersions(sharedStepId, v1, v2);
        return ResponseEntity.ok(diff);
    }
}