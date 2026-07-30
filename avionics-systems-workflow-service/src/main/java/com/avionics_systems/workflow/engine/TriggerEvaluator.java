package com.avionics_systems.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.workflow.entity.WorkflowTransitionTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Evaluates trigger conditions to determine if a trigger should fire.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TriggerEvaluator {

    private final ObjectMapper objectMapper;

    /**
     * Evaluate whether a trigger should fire for the given event.
     *
     * @param trigger The trigger to evaluate
     * @param event  The event that occurred
     * @return true if the trigger should fire
     */
    public boolean evaluateTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        if (trigger == null || event == null) {
            return false;
        }

        log.debug("Evaluating trigger {} for event type {}", trigger.getId(), event.getEventType());

        // Check cooldown first
        if (isInCooldown(trigger)) {
            log.debug("Trigger {} is in cooldown period", trigger.getId());
            return false;
        }

        // Check max fire count
        if (hasReachedMaxFires(trigger)) {
            log.debug("Trigger {} has reached max fire count", trigger.getId());
            return false;
        }

        // Check if trigger type matches event type
        if (!matchesTriggerType(trigger, event)) {
            return false;
        }

        // Evaluate type-specific conditions
        return switch (trigger.getTriggerType()) {
            case WorkflowTransitionTrigger.TRIGGER_TYPE_FIELD_CHANGE -> evaluateFieldChangeTrigger(trigger, event);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_COMMENT_ADDED -> evaluateCommentAddedTrigger(trigger, event);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_DATE_BASED -> evaluateDateTrigger(trigger, event);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_EXTERNAL_WEBHOOK -> evaluateExternalTrigger(trigger, event);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_STATUS_CHANGE -> evaluateStatusChangeTrigger(trigger, event);
            default -> evaluateGenericTrigger(trigger, event);
        };
    }

    /**
     * Check if trigger type matches the event type.
     */
    private boolean matchesTriggerType(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        String triggerType = trigger.getTriggerType();
        String eventType = event.getEventType();

        // Direct match
        if (triggerType.equals(eventType)) {
            return true;
        }

        // Mapping between trigger types and event types
        return switch (triggerType) {
            case WorkflowTransitionTrigger.TRIGGER_TYPE_FIELD_CHANGE ->
                    eventType.equals(TriggerEvent.TYPE_FIELD_CHANGED) ||
                            eventType.equals(TriggerEvent.TYPE_ISSUE_UPDATED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_COMMENT_ADDED ->
                    eventType.equals(TriggerEvent.TYPE_COMMENT_ADDED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_ATTACHMENT_ADDED ->
                    eventType.equals(TriggerEvent.TYPE_ATTACHMENT_ADDED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_LINK_ADDED ->
                    eventType.equals(TriggerEvent.TYPE_LINK_CREATED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_STATUS_CHANGE ->
                    eventType.equals(TriggerEvent.TYPE_STATUS_CHANGED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_DATE_BASED ->
                    eventType.equals(TriggerEvent.TYPE_DATE_REACHED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_API_TRIGGER ->
                    eventType.equals(TriggerEvent.TYPE_API_CALL);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_SPRINT_START ->
                    eventType.equals(TriggerEvent.TYPE_SPRINT_STARTED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_SPRINT_COMPLETE ->
                    eventType.equals(TriggerEvent.TYPE_SPRINT_COMPLETED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_BUILD_SUCCESS ->
                    eventType.equals(TriggerEvent.TYPE_BUILD_SUCCESS) ||
                            eventType.equals(TriggerEvent.TYPE_BUILD_FAILED);
            case WorkflowTransitionTrigger.TRIGGER_TYPE_PULL_REQUEST ->
                    eventType.equals(TriggerEvent.TYPE_PULL_REQUEST_MERGED);
            default -> true;
        };
    }

    /**
     * Evaluate field change trigger conditions.
     */
    private boolean evaluateFieldChangeTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        try {
            Map<String, Object> config = parseTriggerConfig(trigger);
            if (config == null) {
                return true; // No specific config, fire on any field change
            }

            String configFieldName = (String) config.get("fieldName");
            String operator = (String) config.get("operator");
            Object expectedValue = config.get("value");

            // Get the actual field name from event
            String eventFieldName = event.getMeta(TriggerEvent.META_FIELD_NAME);

            // If config specifies a field, check it matches
            if (configFieldName != null && !configFieldName.equals(eventFieldName)) {
                return false;
            }

            // Evaluate operator
            if (operator != null) {
                return evaluateOperator(operator, event.getPreviousValue(), event.getNewValue(), expectedValue);
            }

            return true;
        } catch (Exception e) {
            log.warn("Error evaluating field change trigger: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Evaluate comment added trigger conditions.
     */
    private boolean evaluateCommentAddedTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        try {
            Map<String, Object> config = parseTriggerConfig(trigger);
            if (config == null) {
                return true;
            }

            String pattern = (String) config.get("pattern");
            Boolean isRegex = (Boolean) config.getOrDefault("regex", false);

            String commentText = event.getMeta(TriggerEvent.META_COMMENT_TEXT);
            if (commentText == null) {
                return false;
            }

            if (pattern == null || pattern.isEmpty()) {
                return true;
            }

            if (Boolean.TRUE.equals(isRegex)) {
                return Pattern.matches(pattern, commentText);
            } else {
                return commentText.contains(pattern);
            }
        } catch (Exception e) {
            log.warn("Error evaluating comment added trigger: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Evaluate date-based trigger conditions.
     */
    private boolean evaluateDateTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        try {
            Map<String, Object> config = parseTriggerConfig(trigger);
            if (config == null) {
                return true;
            }

            String dateField = (String) config.get("dateField");
            Integer offsetMinutes = (Integer) config.getOrDefault("offsetMinutes", 0);

            // Verify the event is for the expected date field
            String eventFieldName = event.getMeta(TriggerEvent.META_FIELD_NAME);
            if (dateField != null && !dateField.equals(eventFieldName)) {
                return false;
            }

            // Check if date was reached (event timestamp vs target date)
            Object newValue = event.getNewValue();
            if (newValue instanceof LocalDateTime targetDate) {
                LocalDateTime now = LocalDateTime.now();
                return now.isAfter(targetDate.minusMinutes(offsetMinutes));
            }

            return true;
        } catch (Exception e) {
            log.warn("Error evaluating date trigger: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Evaluate external webhook trigger conditions.
     */
    private boolean evaluateExternalTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        try {
            Map<String, Object> config = parseTriggerConfig(trigger);
            if (config == null) {
                return true;
            }

            String expectedSource = (String) config.get("expectedSource");
            String webhookId = (String) config.get("webhookId");

            // Check webhook source if configured
            String eventSource = event.getMeta(TriggerEvent.META_WEBHOOK_SOURCE);
            if (expectedSource != null && !expectedSource.equals(eventSource)) {
                return false;
            }

            // Verify webhook ID if configured
            if (webhookId != null) {
                String eventWebhookId = event.getMeta("webhookId");
                if (!webhookId.equals(eventWebhookId)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("Error evaluating external webhook trigger: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Evaluate status change trigger conditions.
     */
    private boolean evaluateStatusChangeTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        try {
            Map<String, Object> config = parseTriggerConfig(trigger);
            if (config == null) {
                return true;
            }

            String expectedFromStatus = (String) config.get("fromStatus");
            String expectedToStatus = (String) config.get("toStatus");

            // Check from status
            if (expectedFromStatus != null) {
                Object fromStatus = event.getPreviousValue();
                if (!expectedFromStatus.equals(fromStatus)) {
                    return false;
                }
            }

            // Check to status
            if (expectedToStatus != null) {
                Object toStatus = event.getNewValue();
                if (!expectedToStatus.equals(toStatus)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("Error evaluating status change trigger: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generic trigger evaluation - evaluates any additional conditions.
     */
    private boolean evaluateGenericTrigger(WorkflowTransitionTrigger trigger, TriggerEvent event) {
        // Evaluate additional conditions if present
        String conditions = trigger.getConditions();
        if (conditions == null || conditions.isEmpty() || conditions.equals("[]")) {
            return true;
        }

        try {
            List<Map<String, Object>> conditionList = objectMapper.readValue(
                    conditions, new TypeReference<List<Map<String, Object>>>() {});

            if (conditionList.isEmpty()) {
                return true;
            }

            for (Map<String, Object> condition : conditionList) {
                if (!evaluateCondition(condition, event)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("Error evaluating generic trigger conditions: {}", e.getMessage());
            return true; // Default to fire on error
        }
    }

    /**
     * Evaluate a single condition from the conditions list.
     */
    private boolean evaluateCondition(Map<String, Object> condition, TriggerEvent event) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object value = condition.get("value");

        if (field == null) {
            return true;
        }

        Object fieldValue = getFieldValue(event, field);
        return evaluateOperator(operator, fieldValue, value);
    }

    /**
     * Get a field value from the trigger event.
     */
    private Object getFieldValue(TriggerEvent event, String field) {
        return switch (field) {
            case "previousValue" -> event.getPreviousValue();
            case "newValue" -> event.getNewValue();
            case "eventType" -> event.getEventType();
            case "issueId" -> event.getIssueId();
            default -> event.getMeta(field);
        };
    }

    /**
     * Evaluate an operator against values.
     */
    private boolean evaluateOperator(String operator, Object previousValue, Object newValue, Object expectedValue) {
        return evaluateOperator(operator, newValue, expectedValue);
    }

    /**
     * Evaluate an operator against current and expected values.
     */
    private boolean evaluateOperator(String operator, Object currentValue, Object expectedValue) {
        if (operator == null) {
            return true;
        }

        return switch (operator) {
            case "EQUALS" -> equalsValue(currentValue, expectedValue);
            case "NOT_EQUALS" -> !equalsValue(currentValue, expectedValue);
            case "CONTAINS" -> containsValue(currentValue, expectedValue);
            case "NOT_CONTAINS" -> !containsValue(currentValue, expectedValue);
            case "STARTS_WITH" -> startsWith(currentValue, expectedValue);
            case "ENDS_WITH" -> endsWith(currentValue, expectedValue);
            case "GREATER_THAN" -> greaterThan(currentValue, expectedValue);
            case "LESS_THAN" -> lessThan(currentValue, expectedValue);
            case "IS_NULL" -> currentValue == null;
            case "IS_NOT_NULL" -> currentValue != null;
            case "IS_EMPTY" -> isEmpty(currentValue);
            case "IS_NOT_EMPTY" -> !isEmpty(currentValue);
            default -> true;
        };
    }

    private boolean equalsValue(Object current, Object expected) {
        if (current == null && expected == null) return true;
        if (current == null) return false;
        return current.equals(expected);
    }

    private boolean containsValue(Object current, Object expected) {
        if (current == null || expected == null) return false;
        return current.toString().contains(expected.toString());
    }

    private boolean startsWith(Object current, Object expected) {
        if (current == null || expected == null) return false;
        return current.toString().startsWith(expected.toString());
    }

    private boolean endsWith(Object current, Object expected) {
        if (current == null || expected == null) return false;
        return current.toString().endsWith(expected.toString());
    }

    @SuppressWarnings("unchecked")
    private boolean greaterThan(Object current, Object expected) {
        if (current == null || expected == null) return false;
        if (current instanceof Comparable && expected instanceof Comparable) {
            return ((Comparable) current).compareTo(expected) > 0;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean lessThan(Object current, Object expected) {
        if (current == null || expected == null) return false;
        if (current instanceof Comparable && expected instanceof Comparable) {
            return ((Comparable) current).compareTo(expected) < 0;
        }
        return false;
    }

    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).isEmpty();
        if (value instanceof java.util.Collection) return ((java.util.Collection<?>) value).isEmpty();
        return false;
    }

    /**
     * Check if the trigger is currently in cooldown period.
     */
    private boolean isInCooldown(WorkflowTransitionTrigger trigger) {
        if (trigger.getLastTriggeredAt() == null || trigger.getCooldownSeconds() == null || trigger.getCooldownSeconds() <= 0) {
            return false;
        }

        LocalDateTime cooldownEnd = trigger.getLastTriggeredAt()
                .plusSeconds(trigger.getCooldownSeconds());
        return LocalDateTime.now().isBefore(cooldownEnd);
    }

    /**
     * Check if the trigger has reached its maximum fire count.
     */
    private boolean hasReachedMaxFires(WorkflowTransitionTrigger trigger) {
        if (trigger.getMaxFireCount() == null || trigger.getMaxFireCount() <= 0) {
            return false;
        }
        return trigger.getTriggerCount() != null && trigger.getTriggerCount() >= trigger.getMaxFireCount();
    }

    /**
     * Parse trigger configuration JSON.
     */
    private Map<String, Object> parseTriggerConfig(WorkflowTransitionTrigger trigger) {
        if (trigger.getTriggerConfig() == null || trigger.getTriggerConfig().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(trigger.getTriggerConfig(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse trigger config: {}", e.getMessage());
            return null;
        }
    }
}