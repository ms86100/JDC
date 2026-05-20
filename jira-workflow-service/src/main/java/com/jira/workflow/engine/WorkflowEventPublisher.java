package com.jira.workflow.engine;

import com.jira.workflow.entity.WorkflowEventOutbox;
import com.jira.workflow.repository.WorkflowEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkflowEventPublisher {

    private final WorkflowEventOutboxRepository outboxRepository;

    public void publishIssueTransitioned(WorkflowContext ctx) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("issueId", ctx.getIssueId().toString());
        payload.put("projectId", ctx.getProjectId() != null ? ctx.getProjectId().toString() : null);
        payload.put("transitionId", ctx.getTransition().getId().toString());
        payload.put("transitionName", ctx.getTransition().getName());
        payload.put("fromStatusId", ctx.getCurrentStatusId().toString());
        payload.put("toStatusId", ctx.getTransition().getToStatusId().toString());
        payload.put("userId", ctx.getUserId() != null ? ctx.getUserId().toString() : null);
        if (ctx.getIssueData() != null) {
            Map<String, Object> issue = ctx.getIssueData();
            if (issue.get("issueKey") != null) payload.put("issueKey", issue.get("issueKey"));
            if (issue.get("summary") != null) payload.put("summary", issue.get("summary"));
            if (issue.get("assigneeId") != null) payload.put("assigneeId", issue.get("assigneeId").toString());
            if (issue.get("reporterId") != null) payload.put("reporterId", issue.get("reporterId").toString());
        }

        outboxRepository.save(WorkflowEventOutbox.builder()
                .eventType(WorkflowEventOutbox.ISSUE_TRANSITIONED)
                .aggregateId(ctx.getIssueId())
                .payload(payload)
                .published(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void publish(UUID aggregateId, String eventType, Map<String, Object> payload) {
        outboxRepository.save(WorkflowEventOutbox.builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(payload)
                .published(false)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
