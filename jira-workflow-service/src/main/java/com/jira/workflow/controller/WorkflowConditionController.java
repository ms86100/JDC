package com.jira.workflow.controller;

import com.jira.workflow.dto.ConditionResponse;
import com.jira.workflow.dto.CreateConditionRequest;
import com.jira.workflow.service.WorkflowConditionService;
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

/**
 * REST controller for managing workflow conditions.
 * Provides CRUD operations for conditions attached to transitions.
 */
@RestController
@RequestMapping("/api/workflow/conditions")
@RequiredArgsConstructor
@Tag(name = "Workflow Conditions", description = "CRUD operations for workflow transition conditions")
public class WorkflowConditionController {

    private final WorkflowConditionService workflowConditionService;

    @GetMapping
    @Operation(
            summary = "List all conditions",
            description = "Returns all conditions. Optionally filter by transitionId."
    )
    public ResponseEntity<List<ConditionResponse>> listAllConditions(
            @Parameter(description = "Filter by transition ID")
            @RequestParam(required = false) UUID transitionId) {

        if (transitionId != null) {
            return ResponseEntity.ok(workflowConditionService.getConditionsByTransition(transitionId));
        }
        // Return empty list if no filter - use /transition/{transitionId} for transition-specific queries
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get condition by ID",
            description = "Returns a specific condition by its ID"
    )
    public ResponseEntity<ConditionResponse> getConditionById(
            @Parameter(description = "Condition ID") @PathVariable UUID id) {

        ConditionResponse response = workflowConditionService.getConditionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transition/{transitionId}")
    @Operation(
            summary = "Get conditions by transition",
            description = "Returns all conditions for a specific transition, ordered by sequence"
    )
    public ResponseEntity<List<ConditionResponse>> getConditionsByTransition(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId) {

        List<ConditionResponse> conditions = workflowConditionService.getConditionsByTransition(transitionId);
        return ResponseEntity.ok(conditions);
    }

    @PostMapping
    @Operation(
            summary = "Create condition",
            description = "Creates a new condition. The transitionId must be provided in the request body."
    )
    public ResponseEntity<ConditionResponse> createCondition(
            @Valid @RequestBody CreateConditionRequest request) {

        ConditionResponse response = workflowConditionService.createCondition(request.getTransitionId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @Operation(
            summary = "Bulk create conditions",
            description = "Creates multiple conditions for a transition in a single request. Useful for importing."
    )
    public ResponseEntity<List<ConditionResponse>> bulkCreateConditions(
            @Parameter(description = "Transition ID for all conditions")
            @RequestParam UUID transitionId,
            @Valid @RequestBody List<CreateConditionRequest> requests) {

        List<ConditionResponse> responses = workflowConditionService.bulkCreateConditions(transitionId, requests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update condition",
            description = "Updates an existing condition. Only provided fields will be updated."
    )
    public ResponseEntity<ConditionResponse> updateCondition(
            @Parameter(description = "Condition ID") @PathVariable UUID id,
            @Valid @RequestBody CreateConditionRequest request) {

        ConditionResponse response = workflowConditionService.updateCondition(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete condition",
            description = "Deletes a condition by ID"
    )
    public ResponseEntity<Void> deleteCondition(
            @Parameter(description = "Condition ID") @PathVariable UUID id) {

        workflowConditionService.deleteCondition(id);
        return ResponseEntity.noContent().build();
    }
}
