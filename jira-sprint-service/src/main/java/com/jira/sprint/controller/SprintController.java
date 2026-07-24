package com.jira.sprint.controller;

import com.jira.sprint.dto.CreateSprintRequest;
import com.jira.sprint.dto.SprintResponse;
import com.jira.sprint.dto.UpdateSprintRequest;
import com.jira.sprint.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DEPRECATED SPRINT CONTROLLER
 *
 * ⚠️ This controller is DEPRECATED. All sprint management has been migrated to
 * jira-plan-service which is the single source of truth for all sprint operations.
 *
 * Legacy endpoints are maintained for backward compatibility only.
 * Please update your integration to use jira-plan-service endpoints:
 *
 *   - POST /api/plans/boards/{boardId}/sprints
 *   - GET  /api/plans/boards/{boardId}/sprints
 *   - POST /api/plans/sprints/{sprintId}/start
 *   - POST /api/plans/sprints/{sprintId}/close
 *   - POST /api/plans/sprints/{sprintId}/issues
 *
 * This controller will be removed in a future release.
 *
 * @deprecated Use jira-plan-service SprintController instead
 */
@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sprints (DEPRECATED)", description = "⚠️ DEPRECATED: Use jira-plan-service instead")
@Deprecated
public class SprintController {

    private static final String DEPRECATION_WARNING =
        "This endpoint is deprecated. Use jira-plan-service instead. See: /api/plans/sprints";

    private final SprintService sprintService;

    @Deprecated
    @PostMapping
    @Operation(summary = "Create sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use POST /api/plans/boards/{boardId}/sprints instead")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sprint created (deprecated)"),
        @ApiResponse(responseCode = "410", description = "Deprecated - use jira-plan-service")
    })
    public ResponseEntity<SprintResponse> createSprint(
            @Valid @RequestBody CreateSprintRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.warn("DEPRECATED: /api/sprints POST called. Use /api/plans/boards/{{boardId}}/sprints");
        SprintResponse response = sprintService.createSprint(request, userId);
        return deprecatedResponse(response, HttpStatus.CREATED);
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Get sprints (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use GET /api/plans/boards/{boardId}/sprints instead")
    public ResponseEntity<List<SprintResponse>> getSprints(
            @RequestParam(required = false) UUID projectId) {
        log.warn("DEPRECATED: /api/sprints GET called. Use /api/plans/boards/{{boardId}}/sprints");
        List<SprintResponse> sprints = projectId != null
                ? sprintService.getSprintsForProject(projectId)
                : sprintService.getSprintsByProject(null);
        return deprecatedListResponse(sprints);
    }

    @Deprecated
    @GetMapping("/active")
    @Operation(summary = "Get active sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use jira-plan-service instead")
    public ResponseEntity<SprintResponse> getActiveSprint(
            @RequestParam UUID projectId) {
        log.warn("DEPRECATED: /api/sprints/active GET called. Use jira-plan-service");
        SprintResponse sprint = sprintService.getActiveSprint(projectId);
        return sprint != null ? deprecatedResponse(sprint, HttpStatus.OK) : ResponseEntity.notFound().build();
    }

    @Deprecated
    @GetMapping("/{sprintId}")
    @Operation(summary = "Get sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use GET /api/plans/sprints/{sprintId} instead")
    public ResponseEntity<SprintResponse> getSprint(
            @PathVariable UUID sprintId) {
        log.warn("DEPRECATED: /api/sprints/{} GET called. Use /api/plans/sprints/{}", sprintId, sprintId);
        return deprecatedResponse(sprintService.getSprint(sprintId), HttpStatus.OK);
    }

    @Deprecated
    @PutMapping("/{sprintId}")
    @Operation(summary = "Update sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use PUT /api/plans/sprints/{sprintId} instead")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable UUID sprintId,
            @Valid @RequestBody UpdateSprintRequest request) {
        log.warn("DEPRECATED: /api/sprints/{} PUT called. Use /api/plans/sprints/{}", sprintId, sprintId);
        return deprecatedResponse(sprintService.updateSprint(sprintId, request), HttpStatus.OK);
    }

    @Deprecated
    @PostMapping("/{sprintId}/start")
    @Operation(summary = "Start sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use POST /api/plans/sprints/{sprintId}/start instead")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable UUID sprintId) {
        log.warn("DEPRECATED: /api/sprints/{}/start POST called. Use /api/plans/sprints/{}/start", sprintId, sprintId);
        return deprecatedResponse(sprintService.startSprint(sprintId), HttpStatus.OK);
    }

    @Deprecated
    @PostMapping("/{sprintId}/complete")
    @Operation(summary = "Complete sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use POST /api/plans/sprints/{sprintId}/close instead")
    public ResponseEntity<SprintResponse> completeSprint(
            @PathVariable UUID sprintId) {
        log.warn("DEPRECATED: /api/sprints/{}/complete POST called. Use /api/plans/sprints/{}/close", sprintId, sprintId);
        return deprecatedResponse(sprintService.completeSprint(sprintId), HttpStatus.OK);
    }

    @Deprecated
    @DeleteMapping("/{sprintId}")
    @Operation(summary = "Delete sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use DELETE /api/plans/sprints/{sprintId} instead")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable UUID sprintId) {
        log.warn("DEPRECATED: /api/sprints/{} DELETE called. Use /api/plans/sprints/{}", sprintId, sprintId);
        sprintService.deleteSprint(sprintId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Sunset", "2026-12-31");
        headers.add("X-Suggested-Route", "/api/plans/sprints/" + sprintId);
        return ResponseEntity.noContent().headers(headers).build();
    }

    @Deprecated
    @PostMapping("/{sprintId}/issues")
    @Operation(summary = "Add issue to sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use POST /api/plans/sprints/{sprintId}/issues instead")
    public ResponseEntity<Void> addIssueToSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID issueId) {
        log.warn("DEPRECATED: /api/sprints/{}/issues POST called. Use /api/plans/sprints/{}/issues", sprintId, sprintId);
        sprintService.addIssueToSprint(sprintId, issueId);
        return deprecationHeaderResponse(HttpStatus.OK);
    }

    @Deprecated
    @DeleteMapping("/{sprintId}/issues/{issueId}")
    @Operation(summary = "Remove issue from sprint (DEPRECATED)",
               description = "⚠️ DEPRECATED. Use DELETE /api/plans/sprints/{sprintId}/issues/{planItemId} instead")
    public ResponseEntity<Void> removeIssueFromSprint(
            @PathVariable UUID sprintId,
            @PathVariable UUID issueId) {
        log.warn("DEPRECATED: /api/sprints/{}/issues/{} DELETE called. Use /api/plans/sprints/{}/issues/{}", sprintId, issueId, sprintId, issueId);
        sprintService.removeIssueFromSprint(sprintId, issueId);
        return deprecationHeaderResponse(HttpStatus.NO_CONTENT);
    }

    private <T> ResponseEntity<T> deprecatedResponse(T body, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Sunset", "2026-12-31");
        headers.add("X-Suggested-Route", "Use jira-plan-service /api/plans/* endpoints");
        headers.add("Warning", DEPRECATION_WARNING);
        return new ResponseEntity<>(body, headers, status);
    }

    private <T> ResponseEntity<List<T>> deprecatedListResponse(List<T> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Sunset", "2026-12-31");
        headers.add("X-Suggested-Route", "Use jira-plan-service /api/plans/boards/* endpoints");
        headers.add("Warning", DEPRECATION_WARNING);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private ResponseEntity<Void> deprecationHeaderResponse(HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Sunset", "2026-12-31");
        headers.add("Warning", DEPRECATION_WARNING);
        return new ResponseEntity<>(headers, status);
    }
}