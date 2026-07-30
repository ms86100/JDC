package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.dto.*;
import com.avionics_systems.workflow.engine.BulkWorkflowTransitionService;
import com.avionics_systems.workflow.engine.WorkflowExecutionEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Canonical runtime API — all issue transitions should use these endpoints.
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow Runtime", description = "Avionics Systems DC-style transition execution")
public class WorkflowRuntimeController {

    private final WorkflowExecutionEngine executionEngine;
    private final BulkWorkflowTransitionService bulkTransitionService;

    @PostMapping("/transitions/execute")
    @Operation(summary = "Execute workflow transition (single entry point)")
    public ResponseEntity<TransitionExecutionResponse> executeTransition(
            @Valid @RequestBody ExecuteTransitionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (request.getUserId() == null) {
            request.setUserId(userId);
        }
        return ResponseEntity.ok(executionEngine.execute(request));
    }

    @PostMapping("/transitions/execute-bulk")
    @Operation(summary = "Execute workflow transitions in bulk (same engine, per-issue transactions)")
    public ResponseEntity<BulkTransitionExecutionResponse> executeBulkTransitions(
            @Valid @RequestBody BulkExecuteTransitionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (request.getUserId() == null) {
            request.setUserId(userId);
        }
        return ResponseEntity.ok(bulkTransitionService.executeBulk(request));
    }

    @GetMapping("/issues/{issueId}/available-transitions")
    @Operation(summary = "List transitions available to the current user")
    public ResponseEntity<AvailableTransitionResponse> getAvailableTransitions(
            @PathVariable UUID issueId,
            @RequestParam UUID projectId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(executionEngine.getAvailableTransitions(issueId, projectId, userId));
    }
}
