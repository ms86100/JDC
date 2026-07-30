package com.avionics_systems.notification.service;

import com.avionics_systems.notification.entity.*;
import com.avionics_systems.notification.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationExecutionService {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationTriggerRepository triggerRepository;
    private final AutomationConditionRepository conditionRepository;
    private final AutomationActionRepository actionRepository;
    private final AutomationLogRepository logRepository;
    private final RestTemplate restTemplate;

    @Value("${avionics-systems.services.workflow-url:http://avionics-systems-workflow-service:8085}")
    private String workflowServiceUrl;

    @Value("${app.automation.default-trigger-type:AUTOMATION}")
    private String defaultTriggerType;

    @Value("${app.automation.status.success:SUCCESS}")
    private String statusSuccess;

    @Value("${app.automation.status.failed:FAILED}")
    private String statusFailed;

    @Value("${app.automation.failure-handling.stop:STOP}")
    private String failureHandlingStop;

    @Async
    public void processEvent(String eventType, UUID issueId, UUID projectId, UUID userId,
                              Map<String, Object> eventData) {
        List<AutomationRule> enabledRules = ruleRepository.findByEnabled(true);

        for (AutomationRule rule : enabledRules) {
            if (!matchesTrigger(rule, eventType, projectId)) continue;

            List<AutomationAction> actions = actionRepository.findByRuleId(rule.getId());

            log.info("Executing automation rule '{}' for event {} on issue {}", rule.getName(), eventType, issueId);

            for (AutomationAction action : actions) {
                if (!Boolean.TRUE.equals(action.getEnabled())) continue;

                try {
                    executeAction(action, issueId, projectId, userId, eventData);
                    logExecution(rule.getId(), statusSuccess, null);
                } catch (Exception e) {
                    log.error("Automation action failed for rule '{}': {}", rule.getName(), e.getMessage());
                    logExecution(rule.getId(), statusFailed, e.getMessage());
                    if (failureHandlingStop.equals(action.getFailureHandling())) break;
                }
            }

            rule.setExecutionCount(rule.getExecutionCount() != null ? rule.getExecutionCount() + 1 : 1);
            rule.setLastExecutedAt(OffsetDateTime.now());
            rule.setLastStatus(statusSuccess);
            ruleRepository.save(rule);
        }
    }

    private boolean matchesTrigger(AutomationRule rule, String eventType, UUID projectId) {
        if (rule.getProjectId() != null && !rule.getProjectId().equals(projectId)) return false;
        return rule.getTriggerType() == null || rule.getTriggerType().equalsIgnoreCase(eventType);
    }

    @SuppressWarnings("unchecked")
    private void executeAction(AutomationAction action, UUID issueId, UUID projectId,
                                UUID userId, Map<String, Object> eventData) {
        String actionType = action.getActionType();
        String configJson = action.getActionConfig();
        Map<String, Object> config = parseConfig(configJson);

        switch (actionType != null ? actionType.toUpperCase() : "") {
            case "EXECUTE_SCRIPT" -> {
                String scriptKey = (String) config.get("scriptKey");
                if (scriptKey == null) {
                    log.warn("EXECUTE_SCRIPT action missing scriptKey in config");
                    return;
                }
                Map<String, Object> context = new HashMap<>();
                context.put("issueId", issueId != null ? issueId.toString() : null);
                context.put("projectId", projectId != null ? projectId.toString() : null);
                context.put("userId", userId != null ? userId.toString() : null);
                if (eventData != null) context.putAll(eventData);

                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    restTemplate.postForObject(
                            workflowServiceUrl + "/api/workflow/scripts/execute-by-key/" + scriptKey,
                            new HttpEntity<>(context, headers),
                            Map.class);
                    log.info("Script '{}' executed via automation", scriptKey);
                } catch (Exception e) {
                    log.error("Failed to execute script '{}' via automation: {}", scriptKey, e.getMessage());
                    throw e;
                }
            }
            case "TRANSITION_ISSUE" -> {
                String transitionId = (String) config.get("transitionId");
                if (transitionId != null && issueId != null) {
                    log.info("Automation transitioning issue {} with transition {}", issueId, transitionId);
                }
            }
            case "ADD_COMMENT" -> {
                String commentText = (String) config.get("comment");
                if (commentText != null && issueId != null) {
                    log.info("Automation adding comment to issue {}", issueId);
                }
            }
            case "SEND_NOTIFICATION" -> {
                log.info("Automation sending notification for issue {}", issueId);
            }
            default -> log.warn("Unknown automation action type: {}", actionType);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void logExecution(UUID ruleId, String status, String errorMessage) {
        try {
            AutomationLog logEntry = AutomationLog.builder()
                    .ruleId(ruleId)
                    .triggerType(defaultTriggerType)
                    .status(status)
                    .errorDetails(errorMessage)
                    .actionsExecuted(1)
                    .actionsFailed(statusFailed.equals(status) ? 1 : 0)
                    .build();
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to log automation execution: {}", e.getMessage());
        }
    }
}
