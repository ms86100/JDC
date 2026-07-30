package com.avionics_systems.test.event.listener;

import com.avionics_systems.test.entity.AuditLog;
import com.avionics_systems.test.event.*;
import com.avionics_systems.test.enums.AuditAction;
import com.avionics_systems.test.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener
    public void onTestEvent(TestEvent event) {
        log.info("AuditLogListener: Recording event: {} for project: {}",
                event.getClass().getSimpleName(), event.getProjectId());
        try {
            AuditAction action = mapEventToAuditAction(event);
            String metadata = serializeEvent(event);

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType("TEST_EVENT")
                    .entityId(event.getEventId())
                    .entityName(event.getClass().getSimpleName())
                    .projectId(event.getProjectId())
                    .metadata(metadata)
                    .status("SUCCESS")
                    .changeDescription("Event: " + event.getClass().getSimpleName())
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit log saved for event: {} with id: {}", event.getClass().getSimpleName(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to save audit log for event: {} - {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    private AuditAction mapEventToAuditAction(TestEvent event) {
        if (event instanceof TestRunUpdatedEvent) {
            return AuditAction.TEST_UPDATED;
        } else if (event instanceof TestExecutionStartedEvent) {
            return AuditAction.EXECUTION_STARTED;
        } else if (event instanceof TestExecutionCompletedEvent) {
            return AuditAction.EXECUTION_COMPLETED;
        } else if (event instanceof TestImportedEvent) {
            return AuditAction.CUCUMBER_IMPORTED;
        } else if (event instanceof DefectLinkedEvent) {
            return AuditAction.DEFECT_LINKED;
        } else if (event instanceof RequirementUpdatedEvent) {
            return AuditAction.REQUIREMENT_LINKED;
        } else if (event instanceof CoverageRecalculatedEvent) {
            return AuditAction.CONFIGURATION_CHANGED;
        }
        return AuditAction.CONFIGURATION_CHANGED;
    }

    private String serializeEvent(TestEvent event) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventId", event.getEventId());
            eventData.put("eventType", event.getClass().getSimpleName());
            eventData.put("projectId", event.getProjectId());
            eventData.put("occurredAt", event.getOccurredAt());

            addEventSpecificFields(eventData, event);

            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
            return "{}";
        }
    }

    private void addEventSpecificFields(Map<String, Object> eventData, TestEvent event) {
        try {
            var getterMethods = event.getClass().getMethods();
            for (var method : getterMethods) {
                if (method.getName().startsWith("get") && !isExcludedMethod(method.getName())) {
                    String fieldName = method.getName().substring(3, 4).toLowerCase()
                            + method.getName().substring(4);
                    Object value = method.invoke(event);
                    if (value != null) {
                        eventData.put(fieldName, value);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to add event-specific fields: {}", e.getMessage());
        }
    }

    private boolean isExcludedMethod(String methodName) {
        return methodName.equals("getEventId") ||
               methodName.equals("getOccurredAt") ||
               methodName.equals("getProjectId") ||
               methodName.equals("getSource") ||
               methodName.equals("getTimestamp") ||
               methodName.equals("getClass") ||
               methodName.equals("getCreationTime") ||
               methodName.equals("getPropagationContext") ||
               methodName.equals("getPublishedPhase") ||
               methodName.equals("isRefreshEvent");
    }
}