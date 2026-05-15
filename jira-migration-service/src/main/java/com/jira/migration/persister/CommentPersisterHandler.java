package com.jira.migration.persister;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.exception.*;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.service.clients.*;
import com.jira.migration.service.clients.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Comment Persister Handler
 * Handles comment entity creation using real comment service calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentPersisterHandler {

    private final EntityStatusRepository entityStatusRepository;
    private final CommentServiceClient commentServiceClient;
    private final IssueServiceClient issueServiceClient;

    // Track created comments for rollback
    private final List<String> createdCommentIds = new ArrayList<>();

    @Transactional(rollbackFor = Exception.class)
    public CommentPersistResult persistComment(Map<String, Object> commentData, UUID jobId) {
        CommentPersistResult result = new CommentPersistResult();

        try {
            String issueKey = (String) commentData.get("issueKey");
            String issueId = (String) commentData.get("issueId");

            if (issueKey == null && issueId == null) {
                throw new IllegalArgumentException("Issue key or ID is required");
            }

            // Resolve issue ID if only key is provided
            if (issueId == null && issueKey != null) {
                issueId = resolveIssueId(issueKey, jobId);
                if (issueId == null) {
                    throw new EntityNotFoundException("Issue", issueKey);
                }
            }

            String body = (String) commentData.get("body");
            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("Comment body is required");
            }

            String authorId = (String) commentData.get("authorId");

            // Sanitize HTML in comment body
            String sanitizedBody = sanitizeHtml(body);

            // Build create comment request
            CreateCommentRequest request = CreateCommentRequest.builder()
                    .body(sanitizedBody)
                    .authorId(authorId)
                    .created(parseDateTime(commentData.get("createdAt")))
                    .build();

            // Call real comment service
            CommentResponse response = addCommentWithRetry(issueId, request);
            String commentId = response.getId();

            // Track for potential rollback
            createdCommentIds.add(commentId);

            // Update entity status
            updateEntityStatus(jobId, issueKey + ":" + commentId, commentId, "COMMENT", true);

            result.setSuccess(true);
            result.setCommentId(UUID.fromString(commentId));
            log.info("Persisted comment {} for issue {}", commentId, issueKey);

        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (EntityNotFoundException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (ServiceClientException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Comment service error: {}", e.getMessage(), e);
            throw new MigrationException("Failed to create comment in service: " + e.getMessage(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Failed to persist comment: {}", e.getMessage(), e);
            throw new MigrationException("Failed to persist comment: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Add comment with retry logic for transient failures.
     */
    private CommentResponse addCommentWithRetry(String issueId, CreateCommentRequest request) {
        int maxRetries = 3;
        long baseDelayMs = 500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return commentServiceClient.addComment(issueId, request);
            } catch (ServiceClientException e) {
                if (e.isRetryable() && attempt < maxRetries) {
                    log.warn("Comment creation failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, maxRetries, baseDelayMs * attempt, e.getMessage());
                    try {
                        Thread.sleep(baseDelayMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new MigrationException("Comment creation failed after " + maxRetries + " attempts");
    }

    /**
     * Batch persist comments for an issue.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CommentPersistResult> batchPersistComments(
            String issueId,
            List<Map<String, Object>> comments,
            UUID jobId) {

        List<CommentPersistResult> results = new ArrayList<>();

        for (Map<String, Object> commentData : comments) {
            commentData.put("issueId", issueId);
            try {
                results.add(persistComment(commentData, jobId));
            } catch (Exception e) {
                CommentPersistResult failure = new CommentPersistResult();
                failure.setSuccess(false);
                failure.setErrorMessage(e.getMessage());
                results.add(failure);
                log.error("Failed to persist comment for issue {}: {}", issueId, e.getMessage());
            }
        }

        return results;
    }

    private String resolveIssueId(String issueKey, UUID jobId) {
        try {
            Optional<IssueResponse> issue = issueServiceClient.getIssueByKey(issueKey);
            return issue.map(IssueResponse::getId).orElse(null);
        } catch (Exception e) {
            log.debug("Could not resolve issue ID for key {}: {}", issueKey, e.getMessage());
            return null;
        }
    }

    private String sanitizeHtml(String html) {
        if (html == null) return null;
        // Basic HTML sanitization - in production use OWASP Java HTML Sanitizer
        return html.replaceAll("<script>", "&lt;script&gt;")
                   .replaceAll("</script>", "&lt;/script&gt;");
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private void updateEntityStatus(UUID jobId, String sourceKey, String targetId,
                                   String entityType, boolean success) {
        try {
            EntityStatus status = entityStatusRepository
                    .findByJobIdAndEntityTypeAndSourceIdentifier(jobId, entityType, sourceKey)
                    .orElse(EntityStatus.builder()
                            .jobId(jobId)
                            .entityType(entityType)
                            .sourceIdentifier(sourceKey)
                            .build());

            status.setTargetId(targetId);
            status.setStatus(success ? "COMPLETED" : "FAILED");
            status.setProcessedAt(LocalDateTime.now());

            entityStatusRepository.save(status);
        } catch (Exception e) {
            log.warn("Failed to update entity status for {}: {}", sourceKey, e.getMessage());
        }
    }

    /**
     * Rollback created comments on failure.
     */
    public void rollbackCreatedComments() {
        log.info("Rolling back {} created comments", createdCommentIds.size());
        for (String commentId : createdCommentIds) {
            try {
                commentServiceClient.deleteComment(commentId);
                log.debug("Rolled back comment: {}", commentId);
            } catch (Exception e) {
                log.error("Failed to rollback comment {}: {}", commentId, e.getMessage());
            }
        }
        createdCommentIds.clear();
    }

    /**
     * Clear rollback tracking.
     */
    public void clearRollbackTracking() {
        createdCommentIds.clear();
    }

    public static class CommentPersistResult {
        private boolean success;
        private UUID commentId;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getCommentId() { return commentId; }
        public void setCommentId(UUID commentId) { this.commentId = commentId; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}