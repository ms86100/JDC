package com.jira.workflow.controller;

import com.jira.workflow.dto.*;
import com.jira.workflow.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow-schemes")
@RequiredArgsConstructor
@Tag(name = "Workflow Schemes", description = "Workflow scheme management endpoints")
public class WorkflowAdminController {

    private final WorkflowSchemeService workflowSchemeService;
    private final WorkflowDraftService workflowDraftService;
    private final WorkflowLayoutService workflowLayoutService;
    private final WorkflowMigrationService workflowMigrationService;

    // ==================== WORKFLOW SCHEME ENDPOINTS ====================

    @PostMapping
    @Operation(summary = "Create workflow scheme", description = "Creates a new workflow scheme")
    public ResponseEntity<WorkflowSchemeResponse> createScheme(
            @Valid @RequestBody CreateWorkflowSchemeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowSchemeResponse response = workflowSchemeService.createScheme(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all workflow schemes", description = "Returns all workflow schemes")
    public ResponseEntity<List<WorkflowSchemeResponse>> listSchemes() {
        List<WorkflowSchemeResponse> schemes = workflowSchemeService.listAllSchemes();
        return ResponseEntity.ok(schemes);
    }

    @GetMapping("/{schemeId}")
    @Operation(summary = "Get workflow scheme", description = "Returns workflow scheme by ID")
    public ResponseEntity<WorkflowSchemeResponse> getScheme(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId) {
        WorkflowSchemeResponse response = workflowSchemeService.getScheme(schemeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{schemeId}")
    @Operation(summary = "Update workflow scheme", description = "Updates an existing workflow scheme")
    public ResponseEntity<WorkflowSchemeResponse> updateScheme(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId,
            @Valid @RequestBody CreateWorkflowSchemeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowSchemeResponse response = workflowSchemeService.updateScheme(schemeId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{schemeId}")
    @Operation(summary = "Delete workflow scheme", description = "Deletes a workflow scheme")
    public ResponseEntity<Void> deleteScheme(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId) {
        workflowSchemeService.deleteScheme(schemeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{schemeId}/mappings")
    @Operation(summary = "Add workflow mapping", description = "Maps an issue type to a workflow in the scheme")
    public ResponseEntity<WorkflowSchemeResponse> addMapping(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId,
            @Valid @RequestBody WorkflowSchemeMappingRequest request) {
        WorkflowSchemeResponse response = workflowSchemeService.addMapping(schemeId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{schemeId}/mappings/{mappingId}")
    @Operation(summary = "Update workflow mapping", description = "Updates a workflow mapping in the scheme")
    public ResponseEntity<WorkflowSchemeResponse> updateMapping(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId,
            @Parameter(description = "Mapping ID") @PathVariable UUID mappingId,
            @Valid @RequestBody WorkflowSchemeMappingRequest request) {
        WorkflowSchemeResponse response = workflowSchemeService.updateMapping(schemeId, mappingId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{schemeId}/mappings/{mappingId}")
    @Operation(summary = "Remove workflow mapping", description = "Removes a workflow mapping from the scheme")
    public ResponseEntity<WorkflowSchemeResponse> removeMapping(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId,
            @Parameter(description = "Mapping ID") @PathVariable UUID mappingId) {
        WorkflowSchemeResponse response = workflowSchemeService.removeMapping(schemeId, mappingId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schemeId}/draft")
    @Operation(summary = "Create draft scheme", description = "Creates a draft copy of the scheme for editing")
    public ResponseEntity<WorkflowSchemeResponse> createDraft(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowSchemeResponse response = workflowSchemeService.createDraft(schemeId, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{schemeId}/publish")
    @Operation(summary = "Publish draft scheme", description = "Publishes a draft scheme to replace the original")
    public ResponseEntity<WorkflowSchemeResponse> publishDraft(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowSchemeResponse response = workflowSchemeService.publishDraft(schemeId, userId);
        return ResponseEntity.ok(response);
    }

    // ==================== WORKFLOW DRAFT ENDPOINTS ====================

    @PostMapping("/workflows/{workflowId}/draft")
    @Operation(summary = "Create workflow draft", description = "Creates a draft copy of the workflow")
    public ResponseEntity<WorkflowDraftResponse> createWorkflowDraft(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowDraftResponse response = workflowDraftService.createDraft(workflowId, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/drafts/{draftId}")
    @Operation(summary = "Get draft", description = "Returns draft by ID")
    public ResponseEntity<WorkflowDraftResponse> getDraft(
            @Parameter(description = "Draft ID") @PathVariable UUID draftId) {
        WorkflowDraftResponse response = workflowDraftService.getDraft(draftId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflows/{workflowId}/draft")
    @Operation(summary = "Get draft for workflow", description = "Returns the active draft for a workflow")
    public ResponseEntity<WorkflowDraftResponse> getDraftForWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {
        WorkflowDraftResponse response = workflowDraftService.getDraftForWorkflow(workflowId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/drafts/{draftId}")
    @Operation(summary = "Update draft", description = "Updates draft data")
    public ResponseEntity<WorkflowDraftResponse> updateDraft(
            @Parameter(description = "Draft ID") @PathVariable UUID draftId,
            @RequestBody String draftData,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowDraftResponse response = workflowDraftService.updateDraft(draftId, draftData, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/drafts/{draftId}/publish")
    @Operation(summary = "Publish draft", description = "Publishes draft to update the original workflow")
    public ResponseEntity<WorkflowResponse> publishDraft(
            @Parameter(description = "Draft ID") @PathVariable UUID draftId,
            @RequestParam(required = false) String changeDescription,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowResponse response = workflowDraftService.publishDraft(draftId, userId, changeDescription);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/drafts/{draftId}/discard")
    @Operation(summary = "Discard draft", description = "Discards and deletes the draft")
    public ResponseEntity<Void> discardDraft(
            @Parameter(description = "Draft ID") @PathVariable UUID draftId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        workflowDraftService.discardDraft(draftId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/workflows/{workflowId}/versions")
    @Operation(summary = "Get version history", description = "Returns version history for a workflow")
    public ResponseEntity<List<WorkflowVersionResponse>> getVersionHistory(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {
        List<WorkflowVersionResponse> versions = workflowDraftService.getVersionHistory(workflowId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/versions/{versionId}")
    @Operation(summary = "Get version", description = "Returns a specific version")
    public ResponseEntity<WorkflowVersionResponse> getVersion(
            @Parameter(description = "Version ID") @PathVariable UUID versionId) {
        WorkflowVersionResponse response = workflowDraftService.getVersion(versionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/workflows/{workflowId}/rollback/{versionNumber}")
    @Operation(summary = "Rollback to version", description = "Rolls back workflow to a specific version")
    public ResponseEntity<WorkflowResponse> rollbackToVersion(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @Parameter(description = "Version number") @PathVariable Integer versionNumber,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowResponse response = workflowDraftService.rollbackToVersion(workflowId, versionNumber, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/workflows/{workflowId}/copy")
    @Operation(summary = "Copy workflow", description = "Creates a copy of the workflow")
    public ResponseEntity<WorkflowResponse> copyWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestParam String newName,
            @RequestParam(required = false) String newDescription,
            @RequestParam(required = false) UUID targetProjectId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowResponse response = workflowDraftService.copyWorkflow(
                workflowId, newName, newDescription, targetProjectId, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ==================== WORKFLOW LAYOUT ENDPOINTS ====================

    @PostMapping("/workflows/{workflowId}/layout")
    @Operation(summary = "Save layout", description = "Saves the visual layout for a workflow")
    public ResponseEntity<WorkflowLayoutResponse> saveLayout(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestBody String layoutData,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowLayoutResponse response = workflowLayoutService.createOrUpdateLayout(workflowId, layoutData, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflows/{workflowId}/layout")
    @Operation(summary = "Get layout", description = "Returns the visual layout for a workflow")
    public ResponseEntity<WorkflowLayoutResponse> getLayout(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowLayoutResponse response = workflowLayoutService.getOrCreateLayout(workflowId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/workflows/{workflowId}/layout/positions")
    @Operation(summary = "Sync designer node positions", description = "Updates node coordinates from React Flow designer")
    public ResponseEntity<WorkflowLayoutResponse> syncLayoutPositions(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @Valid @RequestBody SyncDesignerLayoutRequest request) {
        return ResponseEntity.ok(workflowLayoutService.syncNodePositions(workflowId, request));
    }

    @PostMapping("/workflows/{workflowId}/layout/lock")
    @Operation(summary = "Lock layout", description = "Locks the layout for editing")
    public ResponseEntity<WorkflowLayoutResponse> lockLayout(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowLayoutResponse response = workflowLayoutService.lockLayout(workflowId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/workflows/{workflowId}/layout/unlock")
    @Operation(summary = "Unlock layout", description = "Unlocks the layout")
    public ResponseEntity<WorkflowLayoutResponse> unlockLayout(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowLayoutResponse response = workflowLayoutService.unlockLayout(workflowId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/workflows/{workflowId}/layout/auto")
    @Operation(summary = "Auto layout", description = "Auto-arranges the workflow diagram layout")
    public ResponseEntity<WorkflowLayoutResponse> autoLayout(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        WorkflowLayoutResponse response = workflowLayoutService.autoLayout(workflowId, userId);
        return ResponseEntity.ok(response);
    }

    // ==================== WORKFLOW MIGRATION ENDPOINTS ====================

    @PostMapping("/migrations")
    @Operation(summary = "Create migration", description = "Creates a status migration record")
    public ResponseEntity<WorkflowMigrationResponse> createMigration(
            @Valid @RequestBody CreateMigrationRequest request) {
        WorkflowMigrationResponse response = workflowMigrationService.createMigration(
                request.getWorkflowId(), request.getOldStatusId(), request.getNewStatusId(),
                request.getMigrationType(), request.getUserId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/migrations/{migrationId}/preview")
    @Operation(summary = "Preview migration", description = "Shows which issues will be affected by the migration")
    public ResponseEntity<MigrationPreviewResponse> previewMigration(
            @Parameter(description = "Migration ID") @PathVariable UUID migrationId,
            @RequestParam UUID oldStatusId,
            @RequestParam UUID newStatusId) {
        MigrationPreviewResponse response = workflowMigrationService.previewMigration(
                migrationId, oldStatusId, newStatusId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/migrations/{migrationId}/execute")
    @Operation(summary = "Execute migration", description = "Executes the status migration")
    public ResponseEntity<WorkflowMigrationResponse> executeMigration(
            @Parameter(description = "Migration ID") @PathVariable UUID migrationId) {
        workflowMigrationService.startMigration(migrationId);
        WorkflowMigrationResponse response = workflowMigrationService.executeMigration(migrationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/migrations/{migrationId}")
    @Operation(summary = "Get migration", description = "Returns migration details")
    public ResponseEntity<WorkflowMigrationResponse> getMigration(
            @Parameter(description = "Migration ID") @PathVariable UUID migrationId) {
        WorkflowMigrationResponse response = workflowMigrationService.getMigration(migrationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/migrations/{migrationId}/issues")
    @Operation(summary = "Get migration issues", description = "Returns issues affected by the migration")
    public ResponseEntity<Page<WorkflowMigrationIssueResponse>> getMigrationIssues(
            @Parameter(description = "Migration ID") @PathVariable UUID migrationId,
            Pageable pageable) {
        Page<WorkflowMigrationIssueResponse> issues = workflowMigrationService.getMigrationIssues(migrationId, pageable);
        return ResponseEntity.ok(issues);
    }

    @PostMapping("/migrations/{migrationId}/cancel")
    @Operation(summary = "Cancel migration", description = "Cancels an in-progress migration")
    public ResponseEntity<WorkflowMigrationResponse> cancelMigration(
            @Parameter(description = "Migration ID") @PathVariable UUID migrationId) {
        WorkflowMigrationResponse response = workflowMigrationService.cancelMigration(migrationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/migrations/{migrationId}/retry")
    @Operation(summary = "Retry failed issues", description = "Retries migration for failed issues")
    public ResponseEntity<WorkflowMigrationResponse> retryFailedIssues(
            @Parameter(description = "Migration ID") @PathVariable UUID migrationId) {
        WorkflowMigrationResponse response = workflowMigrationService.retryFailedIssues(migrationId);
        return ResponseEntity.ok(response);
    }
}