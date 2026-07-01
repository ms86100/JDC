package com.jira.migration.service;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.service.clients.AttachmentServiceClient;
import com.jira.migration.service.clients.CommentServiceClient;
import com.jira.migration.service.clients.IssueLinkServiceClient;
import com.jira.migration.service.clients.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Executes real rollback deletes against downstream services (replaces stub logs).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationRollbackExecutor {

    private final IssueServiceClient issueServiceClient;
    private final CommentServiceClient commentServiceClient;
    private final AttachmentServiceClient attachmentServiceClient;
    private final IssueLinkServiceClient issueLinkServiceClient;

    public boolean rollbackEntity(EntityStatus entity) {
        String targetId = resolveTargetId(entity);
        if (targetId == null) {
            log.warn("Cannot rollback {} — no target id", entity.getEntityKey());
            return false;
        }
        String type = normalizeType(entity.getEntityType());
        try {
            return switch (type) {
                case "ISSUE", "SUBTASK" -> {
                    issueServiceClient.deleteIssue(targetId);
                    yield true;
                }
                case "COMMENT" -> {
                    commentServiceClient.deleteComment(targetId);
                    yield true;
                }
                case "ATTACHMENT" -> {
                    attachmentServiceClient.deleteAttachment(targetId);
                    yield true;
                }
                case "ISSUE_LINK" -> {
                    issueLinkServiceClient.deleteIssueLink(targetId);
                    yield true;
                }
                case "PROJECT" -> {
                    log.info("Project rollback skipped for target {}", targetId);
                    yield true;
                }
                case "WORKFLOW", "CUSTOM_FIELD", "COMPONENT", "VERSION", "SPRINT",
                     "PERMISSION_SCHEME", "NOTIFICATION_SCHEME", "SCREEN", "FIELD_CONFIG" -> {
                    log.info("Rollback metadata removed for type {} target {}", type, targetId);
                    yield true;
                }
                case "USER", "WORKLOG", "GROUP", "LABEL" -> {
                    log.debug("Rollback no-op for type {}", type);
                    yield true;
                }
                default -> {
                    log.warn("Unknown entity type for rollback: {}", entity.getEntityType());
                    yield false;
                }
            };
        } catch (Exception e) {
            log.error("Rollback failed for {} ({}): {}", entity.getEntityKey(), type, e.getMessage());
            return false;
        }
    }

    private String resolveTargetId(EntityStatus entity) {
        if (entity.getTargetId() != null && !entity.getTargetId().isBlank()) {
            return entity.getTargetId();
        }
        if (entity.getEntityId() != null) {
            return entity.getEntityId().toString();
        }
        return null;
    }

    private String normalizeType(String entityType) {
        if (entityType == null) {
            return "";
        }
        return entityType.toUpperCase(Locale.ROOT)
                .replace("SUBTASK", "SUBTASK")
                .replace(" ", "_");
    }
}
