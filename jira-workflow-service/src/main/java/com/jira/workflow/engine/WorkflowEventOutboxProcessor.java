package com.jira.workflow.engine;

import com.jira.workflow.entity.WorkflowEventOutbox;
import com.jira.workflow.repository.WorkflowEventOutboxRepository;
import com.jira.workflow.service.ScriptListenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventOutboxProcessor {

    private final WorkflowEventOutboxRepository outboxRepository;
    private final WorkflowOutboxIntegrationClient integrationClient;
    private final ProjectNotificationSchemeClient notificationSchemeClient;
    private final ScriptListenerService scriptListenerService;

    @Value("${jira.workflow.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.workflow.outbox.default-transition-name:Transition}")
    private String defaultTransitionName;

    @Scheduled(fixedDelayString = "${jira.workflow.outbox.poll-interval-ms:5000}")
    @SchedulerLock(name = "WorkflowEventOutboxProcessor_processOutbox", lockAtMostFor = "PT4S", lockAtLeastFor = "PT2S")
    @Transactional
    public void processOutbox() {
        List<WorkflowEventOutbox> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }
        int limit = Math.min(batchSize, pending.size());
        for (int i = 0; i < limit; i++) {
            WorkflowEventOutbox event = pending.get(i);
            try {
                dispatch(event);
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Outbox dispatch failed for event {} ({}): {}", event.getId(), event.getEventType(), e.getMessage());
            }
        }
    }

    private void dispatch(WorkflowEventOutbox event) {
        String eventType = event.getEventType();
        if (WorkflowEventOutbox.ISSUE_TRANSITIONED.equals(eventType)) {
            handleIssueTransitioned(event);
        } else if (WorkflowEventOutbox.WORKFLOW_PUBLISHED.equals(eventType)) {
            log.debug("Workflow published event {} — no downstream consumer configured", event.getId());
        }
        fireScriptListeners(event);
    }

    private void fireScriptListeners(WorkflowEventOutbox event) {
        try {
            Map<String, Object> payload = event.getPayload() != null ? event.getPayload() : Map.of();
            UUID issueId = event.getAggregateId();
            UUID projectId = parseUuid(payload.get("projectId"));
            UUID userId = parseUuid(payload.get("userId"));
            UUID issueTypeId = parseUuid(payload.get("issueTypeId"));

            Map<String, Object> eventData = new HashMap<>(payload);
            eventData.put("eventId", event.getId() != null ? event.getId().toString() : null);
            eventData.put("aggregateType", event.getAggregateType());

            scriptListenerService.fireEvent(
                    event.getEventType(), issueId, projectId, userId, issueTypeId, eventData);
        } catch (Exception e) {
            log.warn("Script listener dispatch failed for event {}: {}", event.getId(), e.getMessage());
        }
    }

    private void handleIssueTransitioned(WorkflowEventOutbox event) {
        Map<String, Object> payload = event.getPayload() != null ? event.getPayload() : Map.of();
        UUID issueId = event.getAggregateId();
        enrichPayloadFromIssueService(issueId, payload);

        String issueKey = stringVal(payload.get("issueKey"), issueId.toString());
        String summary = stringVal(payload.get("summary"), "");
        String transitionName = stringVal(payload.get("transitionName"), defaultTransitionName);
        UUID actorId = parseUuid(payload.get("userId"));

        String title = issueKey + " transitioned";
        String message = String.format("Moved via \"%s\"%s", transitionName,
                summary.isBlank() ? "" : ": " + summary);

        Set<UUID> recipients = new LinkedHashSet<>();
        addRecipient(recipients, payload.get("assigneeId"));
        addRecipient(recipients, payload.get("reporterId"));
        if (actorId != null) {
            recipients.remove(actorId);
        }

        UUID projectId = parseUuid(payload.get("projectId"));
        Set<UUID> schemeRecipients = notificationSchemeClient.resolveRecipients(projectId, "ISSUE_TRANSITIONED");
        recipients.addAll(schemeRecipients);

        for (UUID recipient : recipients) {
            try {
                integrationClient.sendNotification(
                        recipient,
                        "ISSUE_TRANSITIONED",
                        title,
                        message,
                        "ISSUE",
                        issueId);
            } catch (Exception e) {
                log.warn("Notification skipped for user {}: {}", recipient, e.getMessage());
            }
        }

        integrationClient.broadcastIssueEvent(issueId, projectId, "issue.transitioned");
        integrationClient.broadcastIssueEvent(issueId, projectId, "STATUS_CHANGED");

        String indexTitle = issueKey + (summary.isBlank() ? "" : " — " + summary);
        String indexContent = String.format("status transition %s → %s via %s",
                payload.get("fromStatusId"),
                payload.get("toStatusId"),
                transitionName);
        try {
            integrationClient.indexIssue(issueId, indexTitle, indexContent);
        } catch (Exception e) {
            log.warn("Search index skipped for issue {}: {}", issueId, e.getMessage());
        }
    }

    private void enrichPayloadFromIssueService(UUID issueId, Map<String, Object> payload) {
        if (payload.containsKey("issueKey") && payload.containsKey("summary")) {
            return;
        }
        Map<String, Object> issue = integrationClient.fetchIssue(issueId);
        if (issue.isEmpty()) {
            return;
        }
        if (!payload.containsKey("issueKey") && issue.get("issueKey") != null) {
            payload.put("issueKey", issue.get("issueKey"));
        }
        if (!payload.containsKey("summary") && issue.get("summary") != null) {
            payload.put("summary", issue.get("summary"));
        }
        if (!payload.containsKey("assigneeId") && issue.get("assigneeId") != null) {
            payload.put("assigneeId", issue.get("assigneeId"));
        }
        if (!payload.containsKey("reporterId") && issue.get("reporterId") != null) {
            payload.put("reporterId", issue.get("reporterId"));
        }
    }

    private static void addRecipient(Set<UUID> recipients, Object raw) {
        UUID id = parseUuid(raw);
        if (id != null) {
            recipients.add(id);
        }
    }

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String stringVal(Object raw, String fallback) {
        return raw != null ? raw.toString() : fallback;
    }
}
