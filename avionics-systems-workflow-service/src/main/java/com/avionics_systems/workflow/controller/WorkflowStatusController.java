package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.dto.CreateWorkflowStatusRequest;
import com.avionics_systems.workflow.dto.WorkflowStatusResponse;
import com.avionics_systems.workflow.service.WorkflowStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow Statuses", description = "Workflow status management endpoints")
public class WorkflowStatusController {

    private final WorkflowStatusService workflowStatusService;

    @GetMapping("/{workflowId}/statuses")
    @Operation(summary = "Get workflow statuses", description = "Returns all statuses linked to a workflow")
    public ResponseEntity<List<WorkflowStatusResponse>> getWorkflowStatuses(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {
        List<WorkflowStatusResponse> statuses = workflowStatusService.getWorkflowStatuses(workflowId);
        return ResponseEntity.ok(statuses);
    }

    @PostMapping("/{workflowId}/statuses")
    @Operation(summary = "Add status to workflow", description = "Links a global status to a workflow")
    public ResponseEntity<WorkflowStatusResponse> addStatusToWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestBody CreateWorkflowStatusRequest request) {
        WorkflowStatusResponse response = workflowStatusService.addStatusToWorkflow(workflowId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{workflowId}/statuses/{workflowStatusId}")
    @Operation(summary = "Remove status from workflow", description = "Unlinks a status from a workflow")
    public ResponseEntity<Void> removeStatusFromWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @Parameter(description = "Workflow Status Link ID") @PathVariable UUID workflowStatusId) {
        workflowStatusService.removeStatusFromWorkflow(workflowId, workflowStatusId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{workflowId}/statuses/reorder")
    @Operation(summary = "Reorder workflow statuses", description = "Reorders statuses within a workflow")
    public ResponseEntity<List<WorkflowStatusResponse>> reorderWorkflowStatuses(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @RequestBody List<UUID> statusIds) {
        List<WorkflowStatusResponse> statuses = workflowStatusService.reorderWorkflowStatuses(workflowId, statusIds);
        return ResponseEntity.ok(statuses);
    }
}