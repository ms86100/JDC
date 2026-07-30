package com.avionics_systems.test.service;

import com.avionics_systems.test.entity.WorkflowInstance;
import com.avionics_systems.test.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for sending notifications after workflow status transitions.
 * <p>
 * After any status transition on a V&V entity (TechEvent, VVO, BenchDefect, ProblemReport),
 * this service notifies the assignee and any watchers.
 * <p>
 * Currently logs notification events. In production, this would integrate with
 * the admin-service's notification infrastructure (email, in-app notifications, webhooks).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowNotificationService {

    private final WorkflowInstanceRepository workflowInstanceRepo;

    /**
     * Send notifications after a successful status transition.
     *
     * @param entityType     The type of entity that transitioned (e.g., "TechEvent", "VVO", "BenchDefect")
     * @param entityId       The ID of the entity
     * @param entityKey      The issue key of the entity (e.g., "TE-42")
     * @param fromStatus     The previous status
     * @param toStatus       The new status
     * @param transitionedBy The user who triggered the transition
     * @param assigneeId     The current assignee of the entity (may be null)
     * @param comment        Optional comment on the transition
     */
    public void notifyAfterTransition(String entityType, UUID entityId, String entityKey,
                                       String fromStatus, String toStatus,
                                       UUID transitionedBy, UUID assigneeId, String comment) {
        log.info("Sending notification for {} {} transition: {} -> {} (by user {})",
                entityType, entityKey, fromStatus, toStatus, transitionedBy);

        List<UUID> recipients = collectRecipients(entityType, entityId, transitionedBy, assigneeId);

        if (recipients.isEmpty()) {
            log.info("No notification recipients for {} {} transition", entityType, entityKey);
            return;
        }

        String subject = buildSubject(entityType, entityKey, fromStatus, toStatus);
        String body = buildBody(entityType, entityKey, fromStatus, toStatus, transitionedBy, comment);

        for (UUID recipient : recipients) {
            sendNotification(recipient, subject, body, entityType, entityId);
        }

        log.info("Sent {} notifications for {} {} transition {} -> {}",
                recipients.size(), entityType, entityKey, fromStatus, toStatus);
    }

    /**
     * Notify watchers when a critical status is reached (e.g., BLOCKED, CANCELLED).
     */
    public void notifyCriticalStatusReached(String entityType, UUID entityId, String entityKey,
                                             String status, UUID assigneeId) {
        log.warn("CRITICAL STATUS: {} {} reached status {} - notifying all stakeholders",
                entityType, entityKey, status);

        List<UUID> recipients = collectRecipients(entityType, entityId, null, assigneeId);
        String subject = "[CRITICAL] " + entityType + " " + entityKey + " is now " + status;
        String body = "The " + entityType + " " + entityKey + " has reached a critical status: " + status
                + ". Immediate attention may be required.";

        for (UUID recipient : recipients) {
            sendNotification(recipient, subject, body, entityType, entityId);
        }
    }

    /**
     * Notify assignee when they are assigned to a workflow item.
     */
    public void notifyAssignment(String entityType, UUID entityId, String entityKey,
                                  UUID assigneeId, UUID assignedBy) {
        if (assigneeId == null) {
            return;
        }

        String subject = entityType + " " + entityKey + " assigned to you";
        String body = "You have been assigned to " + entityType + " " + entityKey
                + " by user " + assignedBy + ".";

        sendNotification(assigneeId, subject, body, entityType, entityId);
        log.info("Sent assignment notification for {} {} to user {}", entityType, entityKey, assigneeId);
    }

    /**
     * Collect all recipients for a notification.
     * Includes the assignee and any users associated via workflow instances.
     * Excludes the user who triggered the transition (they already know).
     */
    private List<UUID> collectRecipients(String entityType, UUID entityId,
                                          UUID excludeUserId, UUID assigneeId) {
        List<UUID> recipients = new ArrayList<>();

        // Add assignee if present and not the one who triggered the action
        if (assigneeId != null && !assigneeId.equals(excludeUserId)) {
            recipients.add(assigneeId);
        }

        // Look up workflow instances for this entity to find watchers/initiators
        List<WorkflowInstance> instances = workflowInstanceRepo
                .findByEntityTypeAndEntityId(entityType, entityId);

        for (WorkflowInstance instance : instances) {
            // Add the workflow initiator if different from trigger user
            if (instance.getInitiatedBy() != null
                    && !instance.getInitiatedBy().equals(excludeUserId)
                    && !recipients.contains(instance.getInitiatedBy())) {
                recipients.add(instance.getInitiatedBy());
            }

            // Add the assigned-to user from the workflow if different
            if (instance.getAssignedTo() != null
                    && !instance.getAssignedTo().equals(excludeUserId)
                    && !recipients.contains(instance.getAssignedTo())) {
                recipients.add(instance.getAssignedTo());
            }
        }

        return recipients;
    }

    /**
     * Build a notification subject line.
     */
    private String buildSubject(String entityType, String entityKey,
                                 String fromStatus, String toStatus) {
        return entityType + " " + entityKey + ": " + fromStatus + " -> " + toStatus;
    }

    /**
     * Build a notification body with transition details.
     */
    private String buildBody(String entityType, String entityKey,
                              String fromStatus, String toStatus,
                              UUID transitionedBy, String comment) {
        StringBuilder body = new StringBuilder();
        body.append("Status transition on ").append(entityType).append(" ").append(entityKey).append(":\n");
        body.append("  From: ").append(fromStatus).append("\n");
        body.append("  To: ").append(toStatus).append("\n");
        body.append("  By: ").append(transitionedBy).append("\n");
        body.append("  At: ").append(LocalDateTime.now()).append("\n");

        if (comment != null && !comment.isEmpty()) {
            body.append("  Comment: ").append(comment).append("\n");
        }

        return body.toString();
    }

    /**
     * Send a notification to a specific user.
     * Currently logs the notification. In production, this would integrate with
     * the admin-service notification infrastructure (email, WebSocket, etc.).
     */
    private void sendNotification(UUID recipientId, String subject, String body,
                                   String entityType, UUID entityId) {
        // Log the notification for now. Production implementation would call
        // admin-service's notification API or publish to a message queue.
        log.info("NOTIFICATION -> user={}, subject='{}', entityType={}, entityId={}",
                recipientId, subject, entityType, entityId);
        log.debug("NOTIFICATION body: {}", body);
    }
}
