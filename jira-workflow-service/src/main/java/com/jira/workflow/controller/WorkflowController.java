package com.jira.workflow.controller;

import com.jira.workflow.dto.*;
import com.jira.workflow.service.WorkflowService;
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
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "Workflow and transition management endpoints")
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @Operation(summary = "Create a new workflow", description = "Creates a new workflow with optional statuses")
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request) {

        WorkflowResponse response = workflowService.createWorkflow(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all workflows", description = "Returns all available workflows")
    public ResponseEntity<List<WorkflowResponse>> listWorkflows() {
        List<WorkflowResponse> workflows = workflowService.listAllWorkflows();
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project workflows", description = "Returns all workflows for a project")
    public ResponseEntity<List<WorkflowResponse>> getWorkflowsForProject(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {

        List<WorkflowResponse> workflows = workflowService.getWorkflowsForProject(projectId);
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/{workflowId}")
    @Operation(summary = "Get workflow by ID", description = "Returns workflow details by ID")
    public ResponseEntity<WorkflowResponse> getWorkflow(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {

        WorkflowResponse response = workflowService.getWorkflow(workflowId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transitions")
    @Operation(summary = "Add transition", description = "Adds a new transition to a workflow")
    public ResponseEntity<TransitionResponse> addTransition(
            @Valid @RequestBody CreateTransitionRequest request) {

        TransitionResponse response = workflowService.addTransition(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/project/{projectId}/transitions")
    @Operation(summary = "List project transitions", description = "Returns all transitions for the project's default workflow")
    public ResponseEntity<List<TransitionResponse>> getTransitionsForProject(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {

        List<TransitionResponse> transitions = workflowService.getTransitionsForProject(projectId);
        return ResponseEntity.ok(transitions);
    }

    @GetMapping("/project/{projectId}/validate-transition")
    @Operation(summary = "Validate transition", description = "Checks if a transition is allowed in the project's workflow")
    public ResponseEntity<ValidateTransitionResponse> validateTransition(
            @Parameter(description = "Project ID") @PathVariable UUID projectId,
            @Parameter(description = "From status ID") @RequestParam UUID fromStatus,
            @Parameter(description = "To status ID") @RequestParam UUID toStatus) {

        ValidateTransitionResponse response = workflowService.validateTransition(projectId, fromStatus, toStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{workflowId}/allowed-transitions")
    @Operation(summary = "Get allowed transitions", description = "Returns all allowed transitions from a given status")
    public ResponseEntity<List<TransitionResponse>> getAllowedTransitions(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @Parameter(description = "From status ID") @RequestParam UUID fromStatus) {

        List<TransitionResponse> transitions = workflowService.getAllowedTransitions(workflowId, fromStatus);
        return ResponseEntity.ok(transitions);
    }

    @PostMapping("/issues/{issueId}/execute")
    @Operation(summary = "Execute workflow transition", description = "Executes a workflow transition on an issue with condition validation")
    public ResponseEntity<TransitionExecutionResponse> executeTransition(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Transition ID") @RequestParam UUID transitionId,
            @Parameter(description = "User ID performing the transition") @RequestHeader("X-User-Id") UUID userId) {

        TransitionExecutionResponse response = workflowService.executeTransition(issueId, transitionId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{workflowId}/transitions/{transitionId}/validate")
    @Operation(summary = "Validate transition execution", description = "Validates if a transition can be executed without actually performing it")
    public ResponseEntity<TransitionValidationResponse> validateTransitionExecution(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId,
            @Parameter(description = "Issue ID") @RequestParam UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {

        TransitionValidationResponse response = workflowService.validateTransitionExecution(transitionId, userId, issueId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{workflowId}/transitions-with-details")
    @Operation(summary = "Get transitions with details", description = "Returns all transitions with their conditions, validators, and post-functions")
    public ResponseEntity<List<TransitionDetailResponse>> getTransitionsWithDetails(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {

        List<TransitionDetailResponse> transitions = workflowService.getTransitionsWithDetails(workflowId);
        return ResponseEntity.ok(transitions);
    }

    @PostMapping("/transitions/{transitionId}/conditions")
    @Operation(summary = "Add condition to transition", description = "Attaches a condition to a transition")
    public ResponseEntity<ConditionResponse> addCondition(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId,
            @Valid @RequestBody CreateConditionRequest request) {

        request.setTransitionId(transitionId);
        ConditionResponse response = workflowService.addCondition(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/transitions/{transitionId}/validators")
    @Operation(summary = "Add validator to transition", description = "Attaches a validator to a transition")
    public ResponseEntity<ValidatorResponse> addValidator(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId,
            @Valid @RequestBody CreateValidatorRequest request) {

        request.setTransitionId(transitionId);
        ValidatorResponse response = workflowService.addValidator(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/transitions/{transitionId}/post-functions")
    @Operation(summary = "Add post-function to transition", description = "Attaches a post-function to a transition")
    public ResponseEntity<PostFunctionResponse> addPostFunction(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId,
            @Valid @RequestBody CreatePostFunctionRequest request) {

        request.setTransitionId(transitionId);
        PostFunctionResponse response = workflowService.addPostFunction(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}