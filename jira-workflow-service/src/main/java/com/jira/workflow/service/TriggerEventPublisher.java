package com.jira.workflow.service;

import com.jira.workflow.engine.TriggerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Event publisher for workflow triggers.
 * This service publishes trigger events that are consumed by WorkflowTriggerService.
 * It can be called by external services like jira-issue-service to trigger workflow transitions.
 *
 * Usage from other services:
 * <pre>
 * {@code
 * @Autowired
 * private TriggerEventPublisher publisher;
 *
 * // When a field changes
 * publisher.publishFieldChanged(issueId, "status", "In Progress", "Done");
 *
 * // When a comment is added
 * publisher.publishCommentAdded(issueId, "Fixed the bug");
 *
 * // When a deadline is reached
 * publisher.publishDateReached(issueId, "dueDate");
 * }
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final WorkflowTriggerService triggerService;

    /**
     * Publish a field change event.
     */
    public CompletableFuture<Void> publishFieldChanged(UUID issueId, String fieldName,
                                                        Object previousValue, Object newValue) {
        return publishEvent(TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(TriggerEvent.TYPE_FIELD_CHANGED)
                .issueId(issueId)
                .previousValue(previousValue)
                .newValue(newValue)
                .timestamp(LocalDateTime.now())
                .build()
                .withMeta(TriggerEvent.META_FIELD_NAME, fieldName));
    }

    /**
     * Publish an issue updated event.
     */
    public CompletableFuture<Void> publishIssueUpdated(UUID issueId, Map<String, Object> changes) {
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(TriggerEvent.TYPE_ISSUE_UPDATED)
                .issueId(issueId)
                .timestamp(LocalDateTime.now())
                .build();

        if (changes != null) {
            changes.forEach(event::withMeta);
        }

        return publishEvent(event);
    }

    /**
     * Publish a comment added event.
     */
    public CompletableFuture<Void> publishCommentAdded(UUID issueId, String commentText, UUID authorId) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_COMMENT_ADDED, issueId)
                .withMeta(TriggerEvent.META_COMMENT_TEXT, commentText)
                .withMeta(TriggerEvent.META_USER_ID, authorId);

        return publishEvent(event);
    }

    /**
     * Publish an attachment added event.
     */
    public CompletableFuture<Void> publishAttachmentAdded(UUID issueId, String attachmentName,
                                                          UUID attachmentId, UUID authorId) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_ATTACHMENT_ADDED, issueId)
                .withMeta(TriggerEvent.META_ATTACHMENT_NAME, attachmentName)
                .withMeta("attachmentId", attachmentId)
                .withMeta(TriggerEvent.META_USER_ID, authorId);

        return publishEvent(event);
    }

    /**
     * Publish a link created event.
     */
    public CompletableFuture<Void> publishLinkCreated(UUID issueId, UUID linkedIssueId,
                                                      String linkType, UUID userId) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_LINK_CREATED, issueId)
                .withMeta(TriggerEvent.META_LINKED_ISSUE_ID, linkedIssueId)
                .withMeta(TriggerEvent.META_LINK_TYPE, linkType)
                .withMeta(TriggerEvent.META_USER_ID, userId);

        return publishEvent(event);
    }

    /**
     * Publish a status changed event (on linked issue).
     */
    public CompletableFuture<Void> publishStatusChanged(UUID issueId, Object previousStatus,
                                                        Object newStatus, UUID userId) {
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(TriggerEvent.TYPE_STATUS_CHANGED)
                .issueId(issueId)
                .previousValue(previousStatus)
                .newValue(newStatus)
                .timestamp(LocalDateTime.now())
                .build()
                .withMeta(TriggerEvent.META_USER_ID, userId);

        return publishEvent(event);
    }

    /**
     * Publish a date reached event (for deadline triggers).
     */
    public CompletableFuture<Void> publishDateReached(UUID issueId, String dateField, LocalDateTime targetDate) {
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(TriggerEvent.TYPE_DATE_REACHED)
                .issueId(issueId)
                .newValue(targetDate)
                .timestamp(LocalDateTime.now())
                .build()
                .withMeta(TriggerEvent.META_FIELD_NAME, dateField);

        return publishEvent(event);
    }

    /**
     * Publish an API call event.
     */
    public CompletableFuture<Void> publishApiCall(UUID issueId, String action, UUID userId,
                                                  Map<String, Object> params) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_API_CALL, issueId)
                .withMeta("action", action)
                .withMeta(TriggerEvent.META_USER_ID, userId);

        if (params != null) {
            params.forEach(event::withMeta);
        }

        return publishEvent(event);
    }

    /**
     * Publish a sprint started event.
     */
    public CompletableFuture<Void> publishSprintStarted(UUID issueId, UUID sprintId, String sprintName) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_SPRINT_STARTED, issueId)
                .withMeta(TriggerEvent.META_SPRINT_ID, sprintId)
                .withMeta("sprintName", sprintName);

        return publishEvent(event);
    }

    /**
     * Publish a sprint completed event.
     */
    public CompletableFuture<Void> publishSprintCompleted(UUID issueId, UUID sprintId, String sprintName) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_SPRINT_COMPLETED, issueId)
                .withMeta(TriggerEvent.META_SPRINT_ID, sprintId)
                .withMeta("sprintName", sprintName);

        return publishEvent(event);
    }

    /**
     * Publish a build success event.
     */
    public CompletableFuture<Void> publishBuildSuccess(UUID issueId, String buildId, String buildName,
                                                       String repository) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_BUILD_SUCCESS, issueId)
                .withMeta(TriggerEvent.META_BUILD_ID, buildId)
                .withMeta(TriggerEvent.META_BUILD_NAME, buildName)
                .withMeta(TriggerEvent.META_REPOSITORY, repository);

        return publishEvent(event);
    }

    /**
     * Publish a pull request merged event.
     */
    public CompletableFuture<Void> publishPullRequestMerged(UUID issueId, Integer prNumber,
                                                              String repository) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_PULL_REQUEST_MERGED, issueId)
                .withMeta(TriggerEvent.META_PR_NUMBER, prNumber)
                .withMeta(TriggerEvent.META_REPOSITORY, repository);

        return publishEvent(event);
    }

    /**
     * Publish a deployment success event.
     */
    public CompletableFuture<Void> publishDeploymentSuccess(UUID issueId, String environment,
                                                            String deploymentId) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_DEPLOYMENT_SUCCESS, issueId)
                .withMeta(TriggerEvent.META_DEPLOYMENT_ENV, environment)
                .withMeta("deploymentId", deploymentId);

        return publishEvent(event);
    }

    /**
     * Publish an external webhook event.
     */
    public CompletableFuture<Void> publishExternalWebhook(UUID issueId, String webhookSource,
                                                          String webhookId, Map<String, Object> payload) {
        TriggerEvent event = TriggerEvent.create(TriggerEvent.TYPE_API_CALL, issueId)
                .withMeta(TriggerEvent.META_WEBHOOK_SOURCE, webhookSource)
                .withMeta("webhookId", webhookId);

        if (payload != null) {
            payload.forEach(event::withMeta);
        }

        return publishEvent(event);
    }

    /**
     * Generic event publishing - allows custom events.
     */
    public CompletableFuture<Void> publishCustomEvent(String eventType, UUID issueId,
                                                      Object previousValue, Object newValue,
                                                      Map<String, Object> metadata) {
        TriggerEvent event = TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .issueId(issueId)
                .previousValue(previousValue)
                .newValue(newValue)
                .timestamp(LocalDateTime.now())
                .build();

        if (metadata != null) {
            metadata.forEach(event::withMeta);
        }

        return publishEvent(event);
    }

    /**
     * Fire triggers directly by event type (without publishing).
     * Useful for testing or when the event was already processed.
     */
    @Async
    public CompletableFuture<Void> fireTriggersByEventType(String eventType, UUID issueId,
                                                           Map<String, Object> metadata) {
        try {
            triggerService.fireByEventType(eventType, issueId, metadata);
        } catch (Exception e) {
            log.error("Error firing triggers by event type: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Publish event and trigger matching workflow triggers.
     */
    @Async
    private CompletableFuture<Void> publishEvent(TriggerEvent event) {
        try {
            log.info("Publishing trigger event: type={}, issueId={}", event.getEventType(), event.getIssueId());
            triggerService.checkAndFireTriggers(event);
        } catch (Exception e) {
            log.error("Error publishing trigger event: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Spring ApplicationEvent for trigger events.
     * Use this for Spring event-driven architecture if preferred.
     */
    public static class TriggerEvent2 extends org.springframework.context.ApplicationEvent {
        private final TriggerEvent triggerEvent;

        public TriggerEvent2(Object source, TriggerEvent triggerEvent) {
            super(source);
            this.triggerEvent = triggerEvent;
        }

        public TriggerEvent getTriggerEvent() {
            return triggerEvent;
        }
    }

    /**
     * Publish using Spring's ApplicationEvent system.
     * Use this method if you prefer Spring event listeners.
     */
    public void publishSpringEvent(TriggerEvent event) {
        eventPublisher.publishEvent(new TriggerEvent2(this, event));
    }
}