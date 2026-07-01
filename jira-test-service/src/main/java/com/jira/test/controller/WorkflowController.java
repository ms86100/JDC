package com.jira.test.controller;

import com.jira.test.dto.CreateWorkflowDefinitionRequest;
import com.jira.test.dto.CreateWorkflowInstanceRequest;
import com.jira.test.dto.WorkflowInstanceResponse;
import com.jira.test.entity.WorkflowDefinition;
import com.jira.test.entity.WorkflowInstance;
import com.jira.test.service.WorkflowExecutionService;
import com.jira.test.service.WorkflowExecutionService.StateTransition;
import com.jira.test.service.WorkflowExecutionService.ValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "APIs for workflow execution and management")
public class WorkflowController {

    private final WorkflowExecutionService workflowService;

    // ========== Definition Endpoints ==========

    @PostMapping("/definitions")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #request.projectId)")
    @Operation(summary = "Create a new workflow definition")
    public ResponseEntity<WorkflowDefinition> createDefinition(
            @RequestBody CreateWorkflowDefinitionRequest request) {
        WorkflowDefinition definition = workflowService.createDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(definition);
    }

    @GetMapping("/definitions/{id}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Get a workflow definition by ID")
    public ResponseEntity<WorkflowDefinition> getDefinition(@PathVariable UUID id) {
        WorkflowDefinition definition = workflowService.getDefinitionById(id);
        return ResponseEntity.ok(definition);
    }

    @GetMapping("/definitions/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all workflow definitions for a project")
    public ResponseEntity<List<WorkflowDefinition>> getDefinitionsByProject(
            @PathVariable UUID projectId) {
        List<WorkflowDefinition> definitions = workflowService.getDefinitionsByProject(projectId);
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/definitions/project/{projectId}/type/{workflowType}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get workflow definitions by type for a project")
    public ResponseEntity<List<WorkflowDefinition>> getDefinitionsByType(
            @PathVariable UUID projectId,
            @PathVariable String workflowType) {
        List<WorkflowDefinition> definitions = workflowService.getDefinitionsByType(projectId, workflowType);
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/definitions/project/{projectId}/default/{workflowType}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get the default workflow definition for a project and type")
    public ResponseEntity<WorkflowDefinition> getDefaultDefinition(
            @PathVariable UUID projectId,
            @PathVariable String workflowType) {
        WorkflowDefinition definition = workflowService.getDefaultDefinition(projectId, workflowType);
        return ResponseEntity.ok(definition);
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #request.projectId)")
    @Operation(summary = "Update a workflow definition")
    public ResponseEntity<WorkflowDefinition> updateDefinition(
            @PathVariable UUID id,
            @RequestBody CreateWorkflowDefinitionRequest request) {
        WorkflowDefinition definition = workflowService.updateDefinition(id, request);
        return ResponseEntity.ok(definition);
    }

    @DeleteMapping("/definitions/{id}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #id)")
    @Operation(summary = "Delete a workflow definition")
    public ResponseEntity<Void> deleteDefinition(@PathVariable UUID id) {
        workflowService.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/definitions/{id}/activate")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #id)")
    @Operation(summary = "Activate a workflow definition")
    public ResponseEntity<WorkflowDefinition> activateDefinition(@PathVariable UUID id) {
        WorkflowDefinition definition = workflowService.activateDefinition(id);
        return ResponseEntity.ok(definition);
    }

    @PostMapping("/definitions/{id}/deactivate")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #id)")
    @Operation(summary = "Deactivate a workflow definition")
    public ResponseEntity<WorkflowDefinition> deactivateDefinition(@PathVariable UUID id) {
        WorkflowDefinition definition = workflowService.deactivateDefinition(id);
        return ResponseEntity.ok(definition);
    }

    @PostMapping("/definitions/{id}/validate")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Validate a workflow definition")
    public ResponseEntity<ValidationResult> validateDefinition(@PathVariable UUID id) {
        WorkflowDefinition definition = workflowService.getDefinitionById(id);
        ValidationResult result = workflowService.validateWorkflowDefinition(definition);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/definitions/{id}/states")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Get all states in a workflow definition")
    public ResponseEntity<List<String>> getAllStates(@PathVariable UUID id) {
        List<String> states = workflowService.getAllStates(id);
        return ResponseEntity.ok(states);
    }

    // ========== Instance Endpoints ==========

    @PostMapping("/instances")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #request.projectId)")
    @Operation(summary = "Start a new workflow instance")
    public ResponseEntity<WorkflowInstance> startWorkflow(
            @RequestBody CreateWorkflowInstanceRequest request) {
        WorkflowInstance instance = workflowService.startWorkflow(
                request.getDefinitionId(),
                request.getEntityType(),
                request.getEntityId(),
                request.getInitiatedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(instance);
    }

    @GetMapping("/instances/{id}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Get a workflow instance by ID")
    public ResponseEntity<WorkflowInstance> getInstance(@PathVariable UUID id) {
        WorkflowInstance instance = workflowService.getInstanceById(id);
        return ResponseEntity.ok(instance);
    }

    @PutMapping("/instances/{id}/transition")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #id)")
    @Operation(summary = "Transition a workflow instance to a new state")
    public ResponseEntity<WorkflowInstance> transition(
            @PathVariable UUID id,
            @RequestParam String targetState,
            @RequestParam UUID userId,
            @RequestParam(required = false) String comment) {
        WorkflowInstance instance = workflowService.transition(id, targetState, userId, comment);
        return ResponseEntity.ok(instance);
    }

    @PutMapping("/instances/{id}/cancel")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #id)")
    @Operation(summary = "Cancel a workflow instance")
    public ResponseEntity<WorkflowInstance> cancelWorkflow(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestParam(required = false) String reason) {
        WorkflowInstance instance = workflowService.cancelWorkflow(id, userId, reason);
        return ResponseEntity.ok(instance);
    }

    @PutMapping("/instances/{id}/reassign")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #id)")
    @Operation(summary = "Reassign a workflow instance to another user")
    public ResponseEntity<WorkflowInstance> reassignWorkflow(
            @PathVariable UUID id,
            @RequestParam UUID newAssignee,
            @RequestParam UUID userId) {
        WorkflowInstance instance = workflowService.reassignWorkflow(id, newAssignee, userId);
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/instances/{id}/transitions")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Get available transitions for a workflow instance")
    public ResponseEntity<List<String>> getAvailableTransitions(@PathVariable UUID id) {
        List<String> transitions = workflowService.getAvailableTransitions(id);
        return ResponseEntity.ok(transitions);
    }

    @GetMapping("/instances/{id}/progress")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Get workflow progress information")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable UUID id) {
        Map<String, Object> progress = workflowService.getWorkflowProgress(id);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/instances/{id}/history")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Get the state transition history for a workflow instance")
    public ResponseEntity<List<StateTransition>> getStateHistory(@PathVariable UUID id) {
        List<StateTransition> history = workflowService.getStateHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/instances/active")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #userId)")
    @Operation(summary = "Get all active (non-completed) workflow instances")
    public ResponseEntity<List<WorkflowInstance>> getActiveInstances(@RequestParam(required = false) UUID userId) {
        List<WorkflowInstance> instances = workflowService.getActiveInstances();
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/instances/entity/{entityType}/{entityId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #entityId)")
    @Operation(summary = "Get workflow instances for a specific entity")
    public ResponseEntity<List<WorkflowInstance>> getInstancesByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        List<WorkflowInstance> instances = workflowService.getInstancesByEntity(entityType, entityId);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/instances/definition/{definitionId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #definitionId)")
    @Operation(summary = "Get all workflow instances for a definition")
    public ResponseEntity<List<WorkflowInstance>> getInstancesByDefinition(
            @PathVariable UUID definitionId) {
        List<WorkflowInstance> instances = workflowService.getInstancesByDefinition(definitionId);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/instances/user/{userId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #userId)")
    @Operation(summary = "Get workflow instances initiated by a user")
    public ResponseEntity<List<WorkflowInstance>> getInstancesByUser(@PathVariable UUID userId) {
        List<WorkflowInstance> instances = workflowService.getInstancesByUser(userId);
        return ResponseEntity.ok(instances);
    }

    // ========== Exception Handlers ==========

    @ExceptionHandler(WorkflowExecutionService.WorkflowNotFoundException.class)
    @Operation(summary = "Handle workflow not found exception")
    public ResponseEntity<Map<String, String>> handleWorkflowNotFound(
            WorkflowExecutionService.WorkflowNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(WorkflowExecutionService.WorkflowException.class)
    @Operation(summary = "Handle workflow exception")
    public ResponseEntity<Map<String, String>> handleWorkflowException(
            WorkflowExecutionService.WorkflowException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(WorkflowExecutionService.InvalidTransitionException.class)
    @Operation(summary = "Handle invalid transition exception")
    public ResponseEntity<Map<String, String>> handleInvalidTransition(
            WorkflowExecutionService.InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}