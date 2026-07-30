package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.dto.WorkflowTriggerRequest;
import com.avionics_systems.workflow.dto.WorkflowTriggerResponse;
import com.avionics_systems.workflow.service.WorkflowTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for workflow transition triggers.
 * Provides CRUD operations and trigger firing endpoints.
 */
@RestController
@RequestMapping("/api/workflow/triggers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Triggers", description = "Avionics Systems DC-style workflow trigger management")
public class WorkflowTriggerController {

    private final WorkflowTriggerService triggerService;

    /**
     * Create a new trigger for a transition.
     */
    @PostMapping
    @Operation(summary = "Create a new workflow trigger")
    public ResponseEntity<WorkflowTriggerResponse> createTrigger(
            @Valid @RequestBody WorkflowTriggerRequest request) {
        log.info("Creating trigger for transition: {}", request.getTransitionId());
        WorkflowTriggerResponse response = triggerService.createTrigger(request.getTransitionId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a trigger by ID.
     */
    @GetMapping("/{triggerId}")
    @Operation(summary = "Get trigger by ID")
    public ResponseEntity<WorkflowTriggerResponse> getTrigger(@PathVariable UUID triggerId) {
        log.info("Getting trigger: {}", triggerId);
        WorkflowTriggerResponse response = triggerService.getTrigger(triggerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing trigger.
     */
    @PutMapping("/{triggerId}")
    @Operation(summary = "Update a workflow trigger")
    public ResponseEntity<WorkflowTriggerResponse> updateTrigger(
            @PathVariable UUID triggerId,
            @Valid @RequestBody WorkflowTriggerRequest request) {
        log.info("Updating trigger: {}", triggerId);
        WorkflowTriggerResponse response = triggerService.updateTrigger(triggerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a trigger.
     */
    @DeleteMapping("/{triggerId}")
    @Operation(summary = "Delete a workflow trigger")
    public ResponseEntity<Void> deleteTrigger(@PathVariable UUID triggerId) {
        log.info("Deleting trigger: {}", triggerId);
        triggerService.deleteTrigger(triggerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all triggers for a specific transition.
     */
    @GetMapping("/transition/{transitionId}")
    @Operation(summary = "Get all triggers for a transition")
    public ResponseEntity<List<WorkflowTriggerResponse>> getTriggersByTransition(
            @PathVariable UUID transitionId) {
        log.info("Getting triggers for transition: {}", transitionId);
        List<WorkflowTriggerResponse> triggers = triggerService.getTriggersByTransition(transitionId);
        return ResponseEntity.ok(triggers);
    }

    /**
     * Enable a trigger.
     */
    @PostMapping("/{triggerId}/enable")
    @Operation(summary = "Enable a workflow trigger")
    public ResponseEntity<WorkflowTriggerResponse> enableTrigger(@PathVariable UUID triggerId) {
        log.info("Enabling trigger: {}", triggerId);
        WorkflowTriggerResponse response = triggerService.enableTrigger(triggerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Disable a trigger.
     */
    @PostMapping("/{triggerId}/disable")
    @Operation(summary = "Disable a workflow trigger")
    public ResponseEntity<WorkflowTriggerResponse> disableTrigger(@PathVariable UUID triggerId) {
        log.info("Disabling trigger: {}", triggerId);
        WorkflowTriggerResponse response = triggerService.disableTrigger(triggerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Manually fire a specific trigger.
     * This will execute the transition associated with the trigger.
     */
    @PostMapping("/{triggerId}/fire")
    @Operation(summary = "Manually fire a trigger")
    public ResponseEntity<WorkflowTriggerResponse> fireTrigger(
            @PathVariable UUID triggerId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("Manually firing trigger: {} by user: {}", triggerId, userId);
        WorkflowTriggerResponse response = triggerService.fireTrigger(triggerId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Fire triggers by event type.
     * This is useful for testing or when external systems want to trigger workflows.
     */
    @PostMapping("/fire-by-type")
    @Operation(summary = "Fire triggers by event type")
    public ResponseEntity<Map<String, Object>> fireByEventType(@RequestBody FireByEventTypeRequest request) {
        log.info("Firing triggers by event type: {} for issue: {}", request.getEventType(), request.getIssueId());
        List<UUID> firedTriggerIds = triggerService.fireByEventTypeSync(
                request.getEventType(),
                request.getIssueId(),
                request.getMetadata()
        );
        return ResponseEntity.ok(Map.of(
                "eventType", request.getEventType(),
                "issueId", request.getIssueId() != null ? request.getIssueId() : "N/A",
                "firedTriggerIds", firedTriggerIds,
                "count", firedTriggerIds.size()
        ));
    }

    /**
     * Get triggers by type.
     */
    @GetMapping("/type/{triggerType}")
    @Operation(summary = "Get triggers by type")
    public ResponseEntity<List<WorkflowTriggerResponse>> getTriggersByType(@PathVariable String triggerType) {
        log.info("Getting triggers by type: {}", triggerType);
        List<WorkflowTriggerResponse> triggers = triggerService.getTriggersByType(triggerType);
        return ResponseEntity.ok(triggers);
    }

    /**
     * Get all enabled triggers.
     */
    @GetMapping("/enabled")
    @Operation(summary = "Get all enabled triggers")
    public ResponseEntity<List<WorkflowTriggerResponse>> getEnabledTriggers() {
        log.info("Getting all enabled triggers");
        List<WorkflowTriggerResponse> triggers = triggerService.getEnabledTriggers();
        return ResponseEntity.ok(triggers);
    }

    /**
     * Get trigger execution history.
     */
    @GetMapping("/{triggerId}/history")
    @Operation(summary = "Get trigger execution history")
    public ResponseEntity<List<Map<String, Object>>> getTriggerHistory(@PathVariable UUID triggerId) {
        log.info("Getting trigger history: {}", triggerId);
        List<Map<String, Object>> history = triggerService.getTriggerHistory(triggerId);
        return ResponseEntity.ok(history);
    }

    /**
     * Request body for fire-by-type endpoint.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FireByEventTypeRequest {
        private String eventType;
        private UUID issueId;
        private Map<String, Object> metadata;
    }
}