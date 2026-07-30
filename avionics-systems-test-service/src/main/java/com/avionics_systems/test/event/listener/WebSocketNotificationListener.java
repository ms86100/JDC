package com.avionics_systems.test.event.listener;

import com.avionics_systems.test.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void onTestRunUpdated(TestRunUpdatedEvent event) {
        log.info("WebSocketNotificationListener: Broadcasting TestRunUpdatedEvent");
        sendToProjectChannel(event.getProjectId(), "TEST_RUN_UPDATED", buildTestRunPayload(event));
    }

    @Async
    @EventListener
    public void onTestExecutionStarted(TestExecutionStartedEvent event) {
        log.info("WebSocketNotificationListener: Broadcasting TestExecutionStartedEvent");
        sendToProjectChannel(event.getProjectId(), "EXECUTION_STARTED", buildExecutionStartedPayload(event));
    }

    @Async
    @EventListener
    public void onTestExecutionCompleted(TestExecutionCompletedEvent event) {
        log.info("WebSocketNotificationListener: Broadcasting TestExecutionCompletedEvent");
        sendToProjectChannel(event.getProjectId(), "EXECUTION_COMPLETED", buildExecutionCompletedPayload(event));
    }

    @Async
    @EventListener
    public void onCoverageRecalculated(CoverageRecalculatedEvent event) {
        log.info("WebSocketNotificationListener: Broadcasting CoverageRecalculatedEvent");
        sendToProjectChannel(event.getProjectId(), "COVERAGE_UPDATED", buildCoveragePayload(event));
    }

    @Async
    @EventListener
    public void onTestImported(TestImportedEvent event) {
        log.info("WebSocketNotificationListener: Broadcasting TestImportedEvent");
        sendToProjectChannel(event.getProjectId(), "TESTS_IMPORTED", buildImportPayload(event));
    }

    @Async
    @EventListener
    public void onDefectLinked(DefectLinkedEvent event) {
        log.info("WebSocketNotificationListener: Broadcasting DefectLinkedEvent");
        sendToProjectChannel(event.getProjectId(), "DEFECT_LINKED", buildDefectPayload(event));
    }

    @Async
    @EventListener
    public void onRequirementUpdated(RequirementUpdatedEvent event) {
        log.info("WebSocketNotificationListener: Broadcasting RequirementUpdatedEvent");
        sendToProjectChannel(event.getProjectId(), "REQUIREMENT_UPDATED", buildRequirementPayload(event));
    }

    private void sendToProjectChannel(java.util.UUID projectId, String eventType, Map<String, Object> payload) {
        try {
            payload.put("eventType", eventType);
            payload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/test-events/" + projectId, payload);
            log.info("WebSocket notification sent to /topic/test-events/{} - event: {}", projectId, eventType);

            // Also send to general test-events channel for clients subscribed to all events
            messagingTemplate.convertAndSend("/topic/test-events", payload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> buildTestRunPayload(TestRunUpdatedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("executionId", event.getExecutionId());
        payload.put("testId", event.getTestId());
        payload.put("stepId", event.getStepId());
        payload.put("previousStatus", event.getPreviousStatus());
        payload.put("newStatus", event.getNewStatus());
        payload.put("updatedBy", event.getUpdatedBy());
        return payload;
    }

    private Map<String, Object> buildExecutionStartedPayload(TestExecutionStartedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("executionId", event.getExecutionId());
        payload.put("testId", event.getTestId());
        payload.put("testPlanId", event.getTestPlanId());
        payload.put("testSetId", event.getTestSetId());
        payload.put("testerId", event.getTesterId());
        payload.put("testEnv", event.getTestEnv());
        return payload;
    }

    private Map<String, Object> buildExecutionCompletedPayload(TestExecutionCompletedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("executionId", event.getExecutionId());
        payload.put("testId", event.getTestId());
        payload.put("finalStatus", event.getFinalStatus());
        payload.put("passedTests", event.getPassedTests());
        payload.put("failedTests", event.getFailedTests());
        payload.put("blockedTests", event.getBlockedTests());
        payload.put("notRunTests", event.getNotRunTests());
        payload.put("defectKeys", event.getDefectKeys());
        return payload;
    }

    private Map<String, Object> buildCoveragePayload(CoverageRecalculatedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requirementId", event.getRequirementId());
        payload.put("testPlanId", event.getTestPlanId());
        payload.put("coveragePercentage", event.getCoveragePercentage());
        payload.put("totalTests", event.getTotalTests());
        payload.put("coveredTests", event.getCoveredTests());
        return payload;
    }

    private Map<String, Object> buildImportPayload(TestImportedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", event.getBatchId());
        payload.put("importSource", event.getImportSource());
        payload.put("importType", event.getImportType());
        payload.put("totalImported", event.getTotalImported());
        payload.put("successCount", event.getSuccessCount());
        payload.put("failureCount", event.getFailureCount());
        payload.put("testPlanId", event.getTestPlanId());
        return payload;
    }

    private Map<String, Object> buildDefectPayload(DefectLinkedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("executionId", event.getExecutionId());
        payload.put("stepResultId", event.getStepResultId());
        payload.put("defectKey", event.getDefectKey());
        payload.put("severity", event.getSeverity());
        payload.put("linkedBy", event.getLinkedBy());
        return payload;
    }

    private Map<String, Object> buildRequirementPayload(RequirementUpdatedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requirementId", event.getRequirementId());
        payload.put("requirementKey", event.getRequirementKey());
        payload.put("changeType", event.getChangeType());
        payload.put("previousValue", event.getPreviousValue());
        payload.put("newValue", event.getNewValue());
        return payload;
    }
}