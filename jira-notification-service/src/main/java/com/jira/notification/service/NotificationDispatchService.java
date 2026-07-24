package com.jira.notification.service;

import com.jira.notification.dto.NotificationEvent;
import com.jira.notification.entity.NotificationPreference;
import com.jira.notification.entity.NotificationScheme;
import com.jira.notification.entity.NotificationSchemeEvent;
import com.jira.notification.repository.NotificationPreferenceRepository;
import com.jira.notification.repository.NotificationSchemeEventRepository;
import com.jira.notification.repository.NotificationSchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final NotificationSchemeRepository schemeRepository;
    private final NotificationSchemeEventRepository schemeEventRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void dispatchIssueEvent(String eventType, UUID issueId, UUID projectId,
                                   String title, String message, UUID actorUserId) {
        log.info("Dispatching issue event: type={}, issueId={}, projectId={}, actor={}", eventType, issueId, projectId, actorUserId);

        NotificationScheme scheme = resolveScheme(projectId);
        if (scheme == null) {
            log.debug("No notification scheme found for project {}, using direct notification", projectId);
            return;
        }

        List<NotificationSchemeEvent> schemeEvents =
                schemeEventRepository.findActiveEventsForSchemeAndType(scheme.getId(), eventType);

        if (schemeEvents.isEmpty()) {
            log.debug("No active scheme events for type {} in scheme {}", eventType, scheme.getName());
            return;
        }

        Map<String, Object> issueContext = fetchIssueContext(issueId);
        Set<UUID> recipientIds = resolveRecipients(schemeEvents, issueContext);

        if (recipientIds.isEmpty()) {
            log.debug("No recipients resolved for event {} on issue {}", eventType, issueId);
            return;
        }

        if (actorUserId != null) {
            recipientIds.remove(actorUserId);
        }

        Set<UUID> optedOutUsers = getOptedOutUsers(recipientIds, eventType);
        recipientIds.removeAll(optedOutUsers);

        if (recipientIds.isEmpty()) {
            log.debug("All recipients opted out of event type {}", eventType);
            return;
        }

        String resolvedTitle = title != null ? title : formatTitle(eventType, issueContext);
        String resolvedMessage = message != null ? message : formatMessage(eventType, issueId, issueContext);

        int created = 0;
        for (UUID userId : recipientIds) {
            try {
                NotificationEvent event = NotificationEvent.builder()
                        .userId(userId)
                        .type(eventType)
                        .title(resolvedTitle)
                        .message(resolvedMessage)
                        .referenceType("ISSUE")
                        .referenceId(issueId)
                        .build();
                notificationService.createNotification(event);
                created++;
            } catch (Exception e) {
                log.warn("Failed to create notification for user {} on event {}: {}",
                        userId, eventType, e.getMessage());
            }
        }
        log.info("Dispatched {} notifications for event {} on issue {}", created, eventType, issueId);
    }

    private NotificationScheme resolveScheme(UUID projectId) {
        if (projectId == null) {
            return null;
        }
        Optional<NotificationScheme> scheme = schemeRepository.findDefaultByProjectId(projectId);
        if (scheme.isPresent()) {
            return scheme.get();
        }
        List<NotificationScheme> schemes = schemeRepository.findByProjectId(projectId);
        if (!schemes.isEmpty()) {
            return schemes.get(0);
        }
        List<NotificationScheme> defaults = schemeRepository.findAllDefault();
        return defaults.isEmpty() ? null : defaults.get(0);
    }

    private Set<UUID> resolveRecipients(List<NotificationSchemeEvent> schemeEvents,
                                        Map<String, Object> issueContext) {
        Set<UUID> recipients = new LinkedHashSet<>();

        for (NotificationSchemeEvent schemeEvent : schemeEvents) {
            if (schemeEvent.getNotifyAssignee() != null && schemeEvent.getNotifyAssignee()) {
                addUuidFromContext(recipients, issueContext, "assigneeId");
            }
            if (schemeEvent.getNotifyReporter() != null && schemeEvent.getNotifyReporter()) {
                addUuidFromContext(recipients, issueContext, "reporterId");
            }
            if (schemeEvent.getNotifyWatchers() != null && schemeEvent.getNotifyWatchers()) {
                addWatchersFromContext(recipients, issueContext);
            }

            String recipientType = schemeEvent.getRecipientType();
            if (recipientType == null) {
                continue;
            }

            switch (recipientType.toUpperCase()) {
                case "CURRENT_ASSIGNEE":
                    addUuidFromContext(recipients, issueContext, "assigneeId");
                    break;
                case "REPORTER":
                    addUuidFromContext(recipients, issueContext, "reporterId");
                    break;
                case "PROJECT_LEAD":
                    addUuidFromContext(recipients, issueContext, "leadUserId");
                    break;
                case "USER":
                    if (schemeEvent.getRecipientId() != null) {
                        recipients.add(schemeEvent.getRecipientId());
                    }
                    break;
                case "ALL_WATCHERS":
                    addWatchersFromContext(recipients, issueContext);
                    break;
                case "GROUP":
                    if (schemeEvent.getRecipientId() != null) {
                        expandGroupMembers(recipients, schemeEvent.getRecipientId());
                    } else if (schemeEvent.getRecipientGroup() != null) {
                        expandGroupMembersByName(recipients, schemeEvent.getRecipientGroup());
                    }
                    break;
                case "PROJECT_ROLE":
                    if (schemeEvent.getRecipientId() != null) {
                        expandProjectRoleMembers(recipients, schemeEvent.getRecipientId(), issueContext);
                    }
                    break;
                default:
                    if (schemeEvent.getRecipientId() != null) {
                        recipients.add(schemeEvent.getRecipientId());
                    }
                    break;
            }
        }
        return recipients;
    }

    private void addUuidFromContext(Set<UUID> recipients, Map<String, Object> context, String key) {
        Object value = context.get(key);
        if (value instanceof String && !((String) value).isBlank()) {
            try {
                recipients.add(UUID.fromString((String) value));
            } catch (IllegalArgumentException ignored) {
            }
        } else if (value instanceof UUID) {
            recipients.add((UUID) value);
        }
    }

    @SuppressWarnings("unchecked")
    private void addWatchersFromContext(Set<UUID> recipients, Map<String, Object> context) {
        Object watchers = context.get("watchers");
        if (watchers instanceof List) {
            for (Object w : (List<Object>) watchers) {
                if (w instanceof String) {
                    try {
                        recipients.add(UUID.fromString((String) w));
                    } catch (IllegalArgumentException ignored) {
                    }
                } else if (w instanceof Map) {
                    Object userId = ((Map<String, Object>) w).get("userId");
                    if (userId instanceof String) {
                        try {
                            recipients.add(UUID.fromString((String) userId));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }
    }

    private Set<UUID> getOptedOutUsers(Set<UUID> userIds, String eventType) {
        Set<UUID> optedOut = new HashSet<>();
        for (UUID userId : userIds) {
            List<NotificationPreference> prefs = preferenceRepository.findByUserId(userId);
            for (NotificationPreference pref : prefs) {
                if (eventType.equals(pref.getNotificationType()) && !pref.getEnabled()) {
                    optedOut.add(userId);
                    break;
                }
            }
        }
        return optedOut;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchIssueContext(UUID issueId) {
        if (issueId == null) {
            return Collections.emptyMap();
        }
        try {
            String url = String.format("http://jira-issue-service:8084/api/issues/%s", issueId);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch issue context for {}: {}", issueId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String formatTitle(String eventType, Map<String, Object> context) {
        String issueKey = context.getOrDefault("issueKey", "").toString();
        return switch (eventType) {
            case "ISSUE_CREATED" -> issueKey + " has been created";
            case "ISSUE_UPDATED" -> issueKey + " has been updated";
            case "ISSUE_ASSIGNED" -> issueKey + " has been assigned";
            case "ISSUE_COMMENTED" -> issueKey + " has a new comment";
            case "ISSUE_RESOLVED" -> issueKey + " has been resolved";
            case "ISSUE_CLOSED" -> issueKey + " has been closed";
            case "ISSUE_REOPENED" -> issueKey + " has been reopened";
            case "ISSUE_DELETED" -> issueKey + " has been deleted";
            case "ISSUE_MOVED" -> issueKey + " has been moved";
            default -> issueKey + " — " + eventType;
        };
    }

    private String formatMessage(String eventType, UUID issueId, Map<String, Object> context) {
        String issueKey = context.getOrDefault("issueKey", issueId != null ? issueId.toString() : "").toString();
        String title = context.getOrDefault("title", "").toString();
        return String.format("[%s] %s — %s", issueKey, title, eventType.replace("_", " ").toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private void expandGroupMembers(Set<UUID> recipients, UUID groupId) {
        try {
            String url = String.format("http://jira-user-service:8082/rest/admin/1.0/groups/%s/members", groupId);
            List<Map<String, Object>> members = restTemplate.getForObject(url, List.class);
            if (members != null) {
                for (Map<String, Object> member : members) {
                    addUuidFromContext(recipients, member, "userId");
                    addUuidFromContext(recipients, member, "id");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to expand group {} members: {}", groupId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void expandGroupMembersByName(Set<UUID> recipients, String groupName) {
        try {
            String url = String.format("http://jira-user-service:8082/rest/admin/1.0/groups?name=%s", groupName);
            Map<String, Object> group = restTemplate.getForObject(url, Map.class);
            if (group != null && group.get("id") != null) {
                expandGroupMembers(recipients, UUID.fromString(group.get("id").toString()));
            }
        } catch (Exception e) {
            log.warn("Failed to expand group '{}' members: {}", groupName, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void expandProjectRoleMembers(Set<UUID> recipients, UUID roleId, Map<String, Object> issueContext) {
        Object projectIdObj = issueContext.get("projectId");
        if (projectIdObj == null) {
            return;
        }
        try {
            UUID projectId = projectIdObj instanceof UUID ? (UUID) projectIdObj : UUID.fromString(projectIdObj.toString());
            String url = String.format("http://jira-project-service:8083/api/projects/%s/roles/%s/members", projectId, roleId);
            List<Map<String, Object>> members = restTemplate.getForObject(url, List.class);
            if (members != null) {
                for (Map<String, Object> member : members) {
                    addUuidFromContext(recipients, member, "userId");
                    addUuidFromContext(recipients, member, "id");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to expand project role {} members: {}", roleId, e.getMessage());
        }
    }
}
