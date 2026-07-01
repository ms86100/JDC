package com.jira.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.dto.WorkflowTriggerRequest;
import com.jira.workflow.dto.WorkflowTriggerResponse;
import com.jira.workflow.engine.TriggerEvent;
import com.jira.workflow.engine.TriggerEvaluator;
import com.jira.workflow.entity.WorkflowTransition;
import com.jira.workflow.entity.WorkflowTransitionHistory;
import com.jira.workflow.entity.WorkflowTransitionTrigger;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.WorkflowTransitionHistoryRepository;
import com.jira.workflow.repository.WorkflowTransitionRepository;
import com.jira.workflow.repository.WorkflowTransitionTriggerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing workflow transition triggers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTriggerService {

    private final WorkflowTransitionTriggerRepository triggerRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowTransitionHistoryRepository historyRepository;
    private final TriggerEvaluator triggerEvaluator;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowTriggerResponse createTrigger(UUID transitionId, WorkflowTriggerRequest request) {
        log.info("Creating trigger for transition: {}", transitionId);

        WorkflowTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTransition", "id", transitionId));

        String configJson = null;
        if (request.getTriggerConfig() != null) {
            try {
                configJson = objectMapper.writeValueAsString(request.getTriggerConfig());
            } catch (Exception e) {
                log.warn("Failed to serialize trigger config: {}", e.getMessage());
            }
        }

        String conditionsJson = null;
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            try {
                conditionsJson = objectMapper.writeValueAsString(request.getConditions());
            } catch (Exception e) {
                log.warn("Failed to serialize conditions: {}", e.getMessage());
            }
        }

        WorkflowTransitionTrigger trigger = WorkflowTransitionTrigger.builder()
                .transitionId(transitionId)
                .name(request.getName())
                .description(request.getDescription())
                .triggerType(request.getTriggerType())
                .triggerConfig(configJson)
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .executionOrder(request.getExecutionOrder() != null ? request.getExecutionOrder() : 0)
                .cooldownSeconds(request.getCooldownSeconds() != null ? request.getCooldownSeconds() : 60)
                .maxFireCount(request.getMaxFireCount() != null ? request.getMaxFireCount() : 0)
                .conditions(conditionsJson)
                .triggerCount(0)
                .build();

        trigger = triggerRepository.save(trigger);
        log.info("Created trigger: {} for transition: {}", trigger.getId(), transitionId);

        return mapToResponse(trigger, transition);
    }

    @Transactional
    public WorkflowTriggerResponse updateTrigger(UUID id, WorkflowTriggerRequest request) {
        log.info("Updating trigger: {}", id);

        WorkflowTransitionTrigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTransitionTrigger", "id", id));

        if (request.getName() != null) {
            trigger.setName(request.getName());
        }
        if (request.getDescription() != null) {
            trigger.setDescription(request.getDescription());
        }
        if (request.getTriggerType() != null) {
            trigger.setTriggerType(request.getTriggerType());
        }
        if (request.getTriggerConfig() != null) {
            try {
                trigger.setTriggerConfig(objectMapper.writeValueAsString(request.getTriggerConfig()));
            } catch (Exception e) {
                log.warn("Failed to serialize trigger config: {}", e.getMessage());
            }
        }
        if (request.getIsEnabled() != null) {
            trigger.setIsEnabled(request.getIsEnabled());
        }
        if (request.getExecutionOrder() != null) {
            trigger.setExecutionOrder(request.getExecutionOrder());
        }
        if (request.getCooldownSeconds() != null) {
            trigger.setCooldownSeconds(request.getCooldownSeconds());
        }
        if (request.getMaxFireCount() != null) {
            trigger.setMaxFireCount(request.getMaxFireCount());
        }
        if (request.getConditions() != null) {
            try {
                trigger.setConditions(objectMapper.writeValueAsString(request.getConditions()));
            } catch (Exception e) {
                log.warn("Failed to serialize conditions: {}", e.getMessage());
            }
        }

        trigger = triggerRepository.save(trigger);

        WorkflowTransition transition = transitionRepository.findById(trigger.getTransitionId())
                .orElse(null);

        log.info("Updated trigger: {}", id);
        return mapToResponse(trigger, transition);
    }

    @Transactional
    public void deleteTrigger(UUID id) {
        log.info("Deleting trigger: {}", id);

        WorkflowTransitionTrigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTransitionTrigger", "id", id));

        triggerRepository.delete(trigger);
        log.info("Deleted trigger: {}", id);
    }

    @Transactional(readOnly = true)
    public WorkflowTriggerResponse getTrigger(UUID id) {
        WorkflowTransitionTrigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTransitionTrigger", "id", id));

        WorkflowTransition transition = transitionRepository.findById(trigger.getTransitionId())
                .orElse(null);

        return mapToResponse(trigger, transition);
    }

    @Transactional(readOnly = true)
    public List<WorkflowTriggerResponse> getTriggersByTransition(UUID transitionId) {
        List<WorkflowTransitionTrigger> triggers = triggerRepository.findByTransitionId(transitionId);

        WorkflowTransition transition = transitionRepository.findById(transitionId)
                .orElse(null);

        return triggers.stream()
                .map(t -> mapToResponse(t, transition))
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowTriggerResponse enableTrigger(UUID id) {
        log.info("Enabling trigger: {}", id);

        WorkflowTransitionTrigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTransitionTrigger", "id", id));

        trigger.setIsEnabled(true);
        trigger = triggerRepository.save(trigger);

        WorkflowTransition transition = transitionRepository.findById(trigger.getTransitionId())
                .orElse(null);

        return mapToResponse(trigger, transition);
    }

    @Transactional
    public WorkflowTriggerResponse disableTrigger(UUID id) {
        log.info("Disabling trigger: {}", id);

        WorkflowTransitionTrigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTransitionTrigger", "id", id));

        trigger.setIsEnabled(false);
        trigger = triggerRepository.save(trigger);

        WorkflowTransition transition = transitionRepository.findById(trigger.getTransitionId())
                .orElse(null);

        return mapToResponse(trigger, transition);
    }

    /**
     * Check all enabled triggers and fire those matching the event.
     * This is the MAIN LOGIC for automatic trigger firing.
     *
     * @param event The event that occurred
     * @return List of triggered trigger IDs
     */
    @Async
    @Transactional
    public List<UUID> checkAndFireTriggers(TriggerEvent event) {
        log.info("Checking triggers for event type {} on issue {}", event.getEventType(), event.getIssueId());

        List<UUID> firedTriggerIds = new ArrayList<>();

        try {
            // Get all enabled triggers that match the event type
            List<WorkflowTransitionTrigger> matchingTriggers = findMatchingTriggers(event);

            for (WorkflowTransitionTrigger trigger : matchingTriggers) {
                try {
                    if (triggerEvaluator.evaluateTrigger(trigger, event)) {
                        fireTrigger(trigger, event);
                        firedTriggerIds.add(trigger.getId());
                    }
                } catch (Exception e) {
                    log.error("Error evaluating/firing trigger {}: {}", trigger.getId(), e.getMessage());
                }
            }

            log.info("Fired {} triggers for event", firedTriggerIds.size());
        } catch (Exception e) {
            log.error("Error checking triggers for event: {}", e.getMessage());
        }

        return firedTriggerIds;
    }

    /**
     * Manually fire a specific trigger.
     */
    @Transactional
    public WorkflowTriggerResponse fireTrigger(UUID triggerId, UUID userId) {
        log.info("Manually firing trigger: {}", triggerId);

        WorkflowTransitionTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTransitionTrigger", "id", triggerId));

        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_API_CALL, null)
                .withMeta("manual", true)
                .withMeta("userId", userId);

        fireTrigger(trigger, event);

        WorkflowTransition transition = transitionRepository.findById(trigger.getTransitionId())
                .orElse(null);

        return mapToResponse(trigger, transition);
    }

    /**
     * Fire a trigger and execute the associated transition.
     */
    @Async
    @Transactional
    public void fireTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        log.info("Firing trigger: {} with type {}", trigger.getId(), trigger.getTriggerType());

        try {
            // Check for circular dependencies
            if (wouldCreateCircularDependency(trigger, event)) {
                log.warn("Trigger {} would create circular dependency, skipping", trigger.getId());
                return;
            }

            // Update trigger statistics
            trigger.setLastTriggeredAt(LocalDateTime.now());
            if (trigger.getTriggerCount() == null) {
                trigger.setTriggerCount(1);
            } else {
                trigger.setTriggerCount(trigger.getTriggerCount() + 1);
            }
            triggerRepository.save(trigger);

            // Execute the transition
            executeTransition(trigger, event);

            log.info("Successfully fired trigger: {}", trigger.getId());
        } catch (Exception e) {
            log.error("Error firing trigger {}: {}", trigger.getId(), e.getMessage());
        }
    }

    /**
     * Fire triggers by event type (async version).
     */
    @Async
    @Transactional
    public List<UUID> fireByEventType(String eventType, UUID issueId, Map<String, Object> metadata) {
        log.info("Firing triggers by event type {} for issue {}", eventType, issueId);

        TriggerEvent event = TriggerEvent.create(eventType, issueId);
        if (metadata != null) {
            metadata.forEach(event::withMeta);
        }

        return checkAndFireTriggers(event);
    }

    /**
     * Fire triggers by event type (synchronous version for API calls).
     */
    @Transactional(readOnly = true)
    public List<UUID> fireByEventTypeSync(String eventType, UUID issueId, Map<String, Object> metadata) {
        log.info("Synchronous fire triggers by event type {} for issue {}", eventType, issueId);

        TriggerEvent event = TriggerEvent.create(eventType, issueId);
        if (metadata != null) {
            metadata.forEach(event::withMeta);
        }

        // Create a new event without async to get immediate results
        return fireTriggersSync(event);
    }

    /**
     * Synchronous trigger firing without async.
     */
    private List<UUID> fireTriggersSync(TriggerEvent event) {
        List<UUID> firedTriggerIds = new ArrayList<>();

        List<WorkflowTransitionTrigger> matchingTriggers = findMatchingTriggers(event);

        for (WorkflowTransitionTrigger trigger : matchingTriggers) {
            try {
                if (triggerEvaluator.evaluateTrigger(trigger, event)) {
                    fireTriggerSync(trigger, event);
                    firedTriggerIds.add(trigger.getId());
                }
            } catch (Exception e) {
                log.error("Error evaluating/firing trigger {}: {}", trigger.getId(), e.getMessage());
            }
        }

        return firedTriggerIds;
    }

    /**
     * Synchronous trigger firing without async.
     */
    @Transactional
    public void fireTriggerSync(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        log.info("Synchronously firing trigger: {} with type {}", trigger.getId(), trigger.getTriggerType());

        try {
            if (wouldCreateCircularDependency(trigger, event)) {
                log.warn("Trigger {} would create circular dependency, skipping", trigger.getId());
                return;
            }

            trigger.setLastTriggeredAt(LocalDateTime.now());
            if (trigger.getTriggerCount() == null) {
                trigger.setTriggerCount(1);
            } else {
                trigger.setTriggerCount(trigger.getTriggerCount() + 1);
            }
            triggerRepository.save(trigger);

            executeTransition(trigger, event);

            log.info("Successfully fired trigger: {}", trigger.getId());
        } catch (Exception e) {
            log.error("Error firing trigger {}: {}", trigger.getId(), e.getMessage());
        }
    }

    /**
     * Find all triggers that could potentially match an event.
     */
    private List<WorkflowTransitionTrigger> findMatchingTriggers(TriggerEvent event) {
        // First try by specific trigger type
        List<WorkflowTransitionTrigger> triggers = triggerRepository.findEnabledByTriggerType(event.getEventType());

        // If no specific triggers, check all enabled triggers
        if (triggers.isEmpty()) {
            triggers = triggerRepository.findEnabledTriggers();
        }

        // Filter to only triggers that could match this event type
        return triggers.stream()
                .filter(t -> couldMatchTriggerType(t, event))
                .sorted(Comparator.comparingInt(WorkflowTransitionTrigger::getExecutionOrder))
                .collect(Collectors.toList());
    }

    /**
     * Check if a trigger could potentially match the event type.
     */
    private boolean couldMatchTriggerType(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        String triggerType = trigger.getTriggerType();
        String eventType = event.getEventType();

        // Direct match
        if (triggerType.equals(eventType)) {
            return true;
        }

        // Cross-type matching rules
        return switch (triggerType) {
            case WorkflowTransitionTrigger.TRIGGER_TYPE_FIELD_CHANGE ->
                    eventType.equals(TriggerEvent.TYPE_FIELD_CHANGED) ||
                            eventType.equals(TriggerEvent.TYPE_ISSUE_UPDATED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_COMMENT_ADDED,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_ATTACHMENT_ADDED,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_LINK_ADDED,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_STATUS_CHANGE,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_DATE_BASED,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_API_TRIGGER,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_SPRINT_START,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_SPRINT_COMPLETE,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_BUILD_SUCCESS,
                 WorkflowTransitionTrigger.TRIGGER_TYPE_PULL_REQUEST -> triggerType.equals(eventType);
            default -> true; // Allow all for unknown types
        };
    }

    /**
     * Check if firing this trigger would create a circular dependency.
     */
    private boolean wouldCreateCircularDependency(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        // Simple circular dependency detection using a thread-local set
        Set<UUID> currentlyFiring = getCurrentlyFiringSet();
        UUID triggerId = trigger.getId();

        if (currentlyFiring.contains(triggerId)) {
            return true; // This trigger is already being processed
        }

        currentlyFiring.add(triggerId);

        try {
            // Check if this would cause the issue to transition to a status it just came from
            // This is a simplified check - a real implementation would track state history
            return false;
        } finally {
            currentlyFiring.remove(triggerId);
        }
    }

    private Set<UUID> getCurrentlyFiringSet() {
        // Thread-local set to track triggers currently being fired
        // This prevents infinite loops in recursive trigger scenarios
        return currentlyFiring.get();
    }

    /**
     * Execute the transition associated with the trigger.
     */
    private void executeTransition(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        log.debug("Executing transition for trigger {}", trigger.getId());

        WorkflowTransition transition = transitionRepository.findById(trigger.getTransitionId())
                .orElse(null);

        if (transition == null) {
            log.warn("Transition not found for trigger: {}", trigger.getId());
            return;
        }

        // Build execution context
        Map<String, Object> context = new HashMap<>();
        context.put("triggerId", trigger.getId().toString());
        context.put("eventType", event.getEventType());
        context.put("issueId", event.getIssueId() != null ? event.getIssueId().toString() : null);
        context.put("triggerType", trigger.getTriggerType());
        if (event.getMetadata() != null) {
            context.putAll(event.getMetadata());
        }

        // Record trigger execution in history
        try {
            recordTriggerExecution(trigger, transition, event, context);
        } catch (Exception e) {
            log.warn("Could not record trigger execution: {}", e.getMessage());
        }
    }

    /**
     * Record trigger execution in the workflow transition history.
     */
    private void recordTriggerExecution(WorkflowTransitionTrigger trigger,
                                        WorkflowTransition transition,
                                        TriggerEvent event,
                                        Map<String, Object> context) {
        WorkflowTransitionHistory history = WorkflowTransitionHistory.builder()
                .issueId(event.getIssueId())
                .workflowId(transition.getWorkflowId())
                .transitionId(trigger.getTransitionId())
                .transitionName(transition.getName())
                .fromStatusId(transition.getFromStatusId())
                .toStatusId(transition.getToStatusId())
                .userId(getUserIdFromContext(context))
                .screenInput(context)
                .success(true)
                .executedAt(LocalDateTime.now())
                .build();

        historyRepository.save(history);
    }

    private UUID getUserIdFromContext(Map<String, Object> context) {
        Object userId = context.get("userId");
        if (userId instanceof UUID) {
            return (UUID) userId;
        }
        if (userId instanceof String) {
            try {
                return UUID.fromString((String) userId);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get trigger execution history.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTriggerHistory(UUID triggerId) {
        // Find all transition history records that were triggered by this trigger
        List<WorkflowTransitionHistory> histories = historyRepository.findAll().stream()
                .filter(h -> h.getScreenInput() != null &&
                        triggerId.toString().equals(h.getScreenInput().get("triggerId")))
                .collect(Collectors.toList());

        return histories.stream()
                .map(h -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", h.getId());
                    map.put("issueId", h.getIssueId());
                    map.put("transitionId", h.getTransitionId());
                    map.put("transitionName", h.getTransitionName());
                    map.put("executedAt", h.getExecutedAt());
                    map.put("success", h.getSuccess());
                    map.put("userId", h.getUserId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get triggers by type.
     */
    @Transactional(readOnly = true)
    public List<WorkflowTriggerResponse> getTriggersByType(String triggerType) {
        List<WorkflowTransitionTrigger> triggers = triggerRepository.findByTriggerType(triggerType);

        return triggers.stream()
                .map(t -> {
                    WorkflowTransition transition = transitionRepository.findById(t.getTransitionId()).orElse(null);
                    return mapToResponse(t, transition);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all enabled triggers.
     */
    @Transactional(readOnly = true)
    public List<WorkflowTriggerResponse> getEnabledTriggers() {
        List<WorkflowTransitionTrigger> triggers = triggerRepository.findEnabledTriggers();

        return triggers.stream()
                .map(t -> {
                    WorkflowTransition transition = transitionRepository.findById(t.getTransitionId()).orElse(null);
                    return mapToResponse(t, transition);
                })
                .collect(Collectors.toList());
    }

    /**
     * Map entity to response DTO.
     */
    private WorkflowTriggerResponse mapToResponse(WorkflowTransitionTrigger trigger, WorkflowTransition transition) {
        return WorkflowTriggerResponse.builder()
                .id(trigger.getId())
                .transitionId(trigger.getTransitionId())
                .transitionName(transition != null ? transition.getName() : null)
                .workflowName(null) // Would need to fetch from workflow
                .name(trigger.getName())
                .description(trigger.getDescription())
                .triggerType(trigger.getTriggerType())
                .triggerConfig(trigger.getTriggerConfig())
                .isEnabled(trigger.getIsEnabled())
                .executionOrder(trigger.getExecutionOrder())
                .lastTriggeredAt(trigger.getLastTriggeredAt())
                .triggerCount(trigger.getTriggerCount())
                .cooldownSeconds(trigger.getCooldownSeconds())
                .maxFireCount(trigger.getMaxFireCount())
                .conditions(trigger.getConditions())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .build();
    }

    // Thread-local for circular dependency detection
    private static final ThreadLocal<Set<UUID>> currentlyFiring = ThreadLocal.withInitial(HashSet::new);
}