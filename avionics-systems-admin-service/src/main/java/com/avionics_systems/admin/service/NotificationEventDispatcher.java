package com.avionics_systems.admin.service;

import com.avionics_systems.admin.entity.*;
import com.avionics_systems.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Notification Event Dispatcher - Avionics Systems DC compliant event notification system.
 * Handles all event-based notifications based on notification schemes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventDispatcher {

    private final NotificationSchemeRepository notificationSchemeRepository;
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationSchemeEventRepository notificationSchemeEventRepository;
    private final ProjectNotificationSchemeRepository projectNotificationSchemeRepository;
    private final ProjectRepository projectRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final ProjectRoleActorRepository projectRoleActorRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Standard Avionics Systems event keys
    public static final String EVENT_ISSUE_CREATED = "evt-issue_created";
    public static final String EVENT_ISSUE_UPDATED = "evt-issue_updated";
    public static final String EVENT_ISSUE_DELETED = "evt-issue_deleted";
    public static final String EVENT_ISSUE_ASSIGNED = "evt-issue_assigned";
    public static final String EVENT_ISSUE_RESOLVED = "evt-issue_resolved";
    public static final String EVENT_ISSUE_CLOSED = "evt-issue_closed";
    public static final String EVENT_COMMENT_CREATED = "evt-comment_created";
    public static final String EVENT_COMMENT_UPDATED = "evt-comment_updated";
    public static final String EVENT_SPRINT_STARTED = "evt-sprint_started";
    public static final String EVENT_SPRINT_COMPLETED = "evt-sprint_completed";
    public static final String EVENT_EPIC_CREATED = "evt-epic_created";
    public static final String EVENT_EPIC_COMPLETED = "evt-epic_completed";

    // Notification types
    public static final String TYPE_USER = "USER";
    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_PROJECT_ROLE = "PROJECT_ROLE";
    public static final String TYPE_CURRENT_USER = "CURRENT_USER";
    public static final String TYPE_REPORTER = "REPORTER";
    public static final String TYPE_ASSIGNEE = "ASSIGNEE";

    /**
     * Dispatch notification for an issue event.
     * This is the main entry point for all issue-related notifications.
     */
    @Transactional(readOnly = true)
    public void dispatchIssueEvent(String eventKey, IssueEventContext context) {
        String projectId = context.getProjectId();
        String currentUserId = context.getCurrentUserId();

        // Get project's notification scheme
        Optional<String> schemeId = getProjectNotificationSchemeId(projectId);
        if (schemeId.isEmpty()) {
            log.debug("No notification scheme for project {}", projectId);
            return;
        }

        // Get the event ID
        Optional<NotificationEventEntity> event = notificationEventRepository.findByEventKey(eventKey);
        if (event.isEmpty()) {
            log.warn("Unknown event key: {}", eventKey);
            return;
        }

        // Get all notification recipients for this event
        List<NotificationSchemeEventEntity> notifications = notificationSchemeEventRepository
                .findBySchemeAndEvent(schemeId.get(), event.get().getId());

        if (notifications.isEmpty()) {
            log.debug("No notifications configured for event {} in scheme {}", eventKey, schemeId.get());
            return;
        }

        // Collect all recipient user IDs
        Set<String> recipientIds = collectRecipients(notifications, context);

        // Publish notification event for each recipient
        for (String recipientId : recipientIds) {
            if (!recipientId.equals(currentUserId)) {  // Don't notify the user who performed the action
                publishNotification(event.get(), context, recipientId);
            }
        }

        log.info("Dispatched {} notification(s) for event {} on issue {}",
                recipientIds.size(), eventKey, context.getIssueId());
    }

    /**
     * Dispatch sprint-related event.
     */
    @Transactional(readOnly = true)
    public void dispatchSprintEvent(String eventKey, SprintEventContext context) {
        dispatchIssueEvent(eventKey, IssueEventContext.builder()
                .projectId(context.getProjectId())
                .issueId(context.getSprintId())
                .currentUserId(context.getCurrentUserId())
                .issueKey(context.getSprintName())
                .build());
    }

    /**
     * Dispatch epic-related event.
     */
    @Transactional(readOnly = true)
    public void dispatchEpicEvent(String eventKey, EpicEventContext context) {
        dispatchIssueEvent(eventKey, IssueEventContext.builder()
                .projectId(context.getProjectId())
                .issueId(context.getEpicId())
                .currentUserId(context.getCurrentUserId())
                .issueKey(context.getEpicName())
                .build());
    }

    // ===== Private helper methods =====

    private Optional<String> getProjectNotificationSchemeId(String projectId) {
        return projectNotificationSchemeRepository.findByProjectId(projectId)
                .map(ProjectNotificationSchemeEntity::getNotificationSchemeId);
    }

    private Set<String> collectRecipients(List<NotificationSchemeEventEntity> notifications, IssueEventContext context) {
        Set<String> recipients = new HashSet<>();

        for (NotificationSchemeEventEntity notification : notifications) {
            List<String> ids = resolveNotificationRecipients(notification, context);
            recipients.addAll(ids);
        }

        return recipients;
    }

    private List<String> resolveNotificationRecipients(NotificationSchemeEventEntity notification, IssueEventContext context) {
        List<String> recipientIds = new ArrayList<>();

        switch (notification.getNotificationType()) {
            case TYPE_USER:
                if (notification.getNotifierId() != null) {
                    recipientIds.add(notification.getNotifierId());
                }
                break;

            case TYPE_GROUP:
                if (notification.getNotifierId() != null) {
                    // Get all users in the group
                    recipientIds.addAll(getGroupMemberIds(notification.getNotifierId()));
                }
                break;

            case TYPE_PROJECT_ROLE:
                if (notification.getNotifierId() != null) {
                    // Get all users with this role in the project
                    recipientIds.addAll(getRoleMemberIds(context.getProjectId(), notification.getNotifierId()));
                }
                break;

            case TYPE_CURRENT_USER:
                if (context.getCurrentUserId() != null) {
                    recipientIds.add(context.getCurrentUserId());
                }
                break;

            case TYPE_REPORTER:
                if (context.getReporterId() != null) {
                    recipientIds.add(context.getReporterId());
                }
                break;

            case TYPE_ASSIGNEE:
                if (context.getAssigneeId() != null) {
                    recipientIds.add(context.getAssigneeId());
                }
                break;
        }

        return recipientIds;
    }

    private Set<String> getGroupMemberIds(String groupId) {
        Set<String> members = new HashSet<>();
        List<UserGroupMembershipEntity> memberships = userGroupMembershipRepository.findByGroupId(groupId);
        for (UserGroupMembershipEntity membership : memberships) {
            members.add(membership.getUserId());
        }
        return members;
    }

    private Set<String> getRoleMemberIds(String projectId, String roleId) {
        Set<String> members = new HashSet<>();

        // Get direct user role assignments
        List<ProjectRoleActorEntity> roleActors = projectRoleActorRepository
                .findByProjectIdAndProjectRoleId(projectId, roleId);

        for (ProjectRoleActorEntity actor : roleActors) {
            if ("USER".equals(actor.getHolderType())) {
                members.add(actor.getHolderId());
            } else if ("GROUP".equals(actor.getHolderType())) {
                // If role is assigned to a group, get all group members
                members.addAll(getGroupMemberIds(actor.getHolderId()));
            }
        }

        return members;
    }

    private void publishNotification(NotificationEventEntity event, IssueEventContext context, String recipientId) {
        NotificationEvent notification = new NotificationEvent(
                event.getEventKey(),
                event.getName(),
                context.getIssueId(),
                context.getIssueKey(),
                context.getProjectId(),
                recipientId,
                context.getCurrentUserId(),
                context.getChangeDetails()
        );

        eventPublisher.publishEvent(notification);
        log.debug("Published notification to user {} for event {} on issue {}",
                recipientId, event.getEventKey(), context.getIssueId());
    }

    // ===== Event Context Classes =====

    @lombok.Data
    @lombok.Builder
    public static class IssueEventContext {
        private String issueId;
        private String issueKey;
        private String projectId;
        private String currentUserId;
        private String reporterId;
        private String assigneeId;
        private String summary;
        private String description;
        private Map<String, Object> changeDetails;
    }

    @lombok.Data
    @lombok.Builder
    public static class SprintEventContext {
        private String sprintId;
        private String sprintName;
        private String projectId;
        private String currentUserId;
    }

    @lombok.Data
    @lombok.Builder
    public static class EpicEventContext {
        private String epicId;
        private String epicName;
        private String projectId;
        private String currentUserId;
    }

    // ===== Spring Application Event =====

    public static class NotificationEvent extends org.springframework.context.ApplicationEvent {
        private final String eventKey;
        private final String eventName;
        private final String issueId;
        private final String issueKey;
        private final String projectId;
        private final String recipientId;
        private final String triggeredBy;
        private final Map<String, Object> changeDetails;

        public NotificationEvent(String eventKey, String eventName, String issueId, String issueKey,
                                  String projectId, String recipientId, String triggeredBy,
                                  Map<String, Object> changeDetails) {
            super(new Object());
            this.eventKey = eventKey;
            this.eventName = eventName;
            this.issueId = issueId;
            this.issueKey = issueKey;
            this.projectId = projectId;
            this.recipientId = recipientId;
            this.triggeredBy = triggeredBy;
            this.changeDetails = changeDetails;
        }
    }
}