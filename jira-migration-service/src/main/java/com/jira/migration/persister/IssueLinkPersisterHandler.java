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

import java.util.*;

/**
 * Issue Link Persister Handler
 * Handles Epic Link, Parent-Child, and other issue relationships using real service calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IssueLinkPersisterHandler {

    private final EntityStatusRepository entityStatusRepository;
    private final IssueServiceClient issueServiceClient;
    private final IssueLinkServiceClient issueLinkServiceClient;

    // Track created issue links for rollback
    private final List<String> createdLinkIds = new ArrayList<>();

    // Entity type constant
    private static final String ENTITY_TYPE_ISSUE_LINK = "ISSUE_LINK";

    // Standard issue link types
    private static final Map<String, String> LINK_TYPE_NAMES = Map.of(
            "Epic Link", "Epic Link",
            "Parent", "Parent",
            "Subtask", "Subtask",
            "Blocks", "blocks",
            "Is Blocked By", "is blocked by",
            "Duplicates", "duplicates",
            "Is Duplicated By", "is duplicated by",
            "Relates", "relates to"
    );

    @Transactional(rollbackFor = Exception.class)
    public IssueLinkPersistResult persistIssueLink(Map<String, Object> linkData, UUID jobId) {
        IssueLinkPersistResult result = new IssueLinkPersistResult();

        try {
            String linkType = (String) linkData.get("linkType");
            String sourceIssueKey = (String) linkData.get("sourceIssueKey");
            String sourceIssueId = (String) linkData.get("sourceIssueId");
            String targetIssueKey = (String) linkData.get("targetIssueKey");
            String targetIssueId = (String) linkData.get("targetIssueId");

            if (linkType == null) {
                throw new IllegalArgumentException("Link type is required");
            }
            if ((sourceIssueKey == null && sourceIssueId == null) ||
                (targetIssueKey == null && targetIssueId == null)) {
                throw new IllegalArgumentException("Source and target issue keys/IDs are required");
            }

            // Resolve issue IDs if keys are provided
            if (sourceIssueId == null && sourceIssueKey != null) {
                sourceIssueId = resolveIssueId(sourceIssueKey, jobId);
                if (sourceIssueId == null) {
                    throw new EntityNotFoundException("Source Issue", sourceIssueKey);
                }
            }

            if (targetIssueId == null && targetIssueKey != null) {
                targetIssueId = resolveIssueId(targetIssueKey, jobId);
                if (targetIssueId == null) {
                    throw new EntityNotFoundException("Target Issue", targetIssueKey);
                }
            }

            // Create the issue link via service
            IssueLinkServiceClient.CreateIssueLinkRequest request =
                    IssueLinkServiceClient.CreateIssueLinkRequest.builder()
                            .sourceIssueId(sourceIssueId)
                            .targetIssueId(targetIssueId)
                            .linkType(linkType)
                            .direction(determineDirection(linkType))
                            .description((String) linkData.get("description"))
                            .build();

            IssueLinkServiceClient.IssueLinkResponse response = createIssueLinkWithRetry(request);
            String linkId = response.getId();

            // Track for potential rollback
            createdLinkIds.add(linkId);

            // Update entity status
            updateEntityStatus(jobId, sourceIssueKey + "->" + targetIssueKey, linkId,
                    ENTITY_TYPE_ISSUE_LINK, true);

            result.setSuccess(true);
            result.setLinkId(UUID.fromString(linkId));

            log.info("Persisted issue link: {} ({} -> {}) [{}]",
                    linkId, sourceIssueKey, targetIssueKey, linkType);

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
            log.error("Issue link service error: {}", e.getMessage(), e);
            throw new MigrationException("Failed to create issue link: " + e.getMessage(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Failed to persist issue link: {}", e.getMessage(), e);
            throw new MigrationException("Failed to persist issue link: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Create issue link with retry logic for transient failures.
     */
    private IssueLinkServiceClient.IssueLinkResponse createIssueLinkWithRetry(
            IssueLinkServiceClient.CreateIssueLinkRequest request) {

        int maxRetries = 3;
        long baseDelayMs = 500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return issueLinkServiceClient.createIssueLink(request);
            } catch (ServiceClientException e) {
                if (e.isRetryable() && attempt < maxRetries) {
                    log.warn("Issue link creation failed (attempt {}/{}), retrying in {}ms: {}",
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
        throw new MigrationException("Issue link creation failed after " + maxRetries + " attempts");
    }

    /**
     * Persist Epic Link (story to epic).
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistEpicLink(String storyKey, String epicKey, UUID jobId) {
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("linkType", "Epic Link");
        linkData.put("sourceIssueKey", storyKey);
        linkData.put("targetIssueKey", epicKey);
        persistIssueLink(linkData, jobId);
    }

    /**
     * Persist Parent-Child relationship.
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistParentChild(String childKey, String parentKey, UUID jobId) {
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("linkType", "Parent");
        linkData.put("sourceIssueKey", childKey);
        linkData.put("targetIssueKey", parentKey);
        persistIssueLink(linkData, jobId);
    }

    /**
     * Persist Subtask relationship.
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistSubtask(String subtaskKey, String parentKey, UUID jobId) {
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("linkType", "Subtask");
        linkData.put("sourceIssueKey", subtaskKey);
        linkData.put("targetIssueKey", parentKey);
        persistIssueLink(linkData, jobId);
    }

    /**
     * Batch persist issue links.
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchPersistLinks(List<Map<String, Object>> links, UUID jobId) {
        int count = 0;
        for (Map<String, Object> link : links) {
            IssueLinkPersistResult result = persistIssueLink(link, jobId);
            if (result.isSuccess()) count++;
        }
        return count;
    }

    private String resolveIssueId(String issueKey, UUID jobId) {
        try {
            IssueResponse issue = issueServiceClient.getIssue(issueKey);
            return issue != null ? issue.getId() : null;
        } catch (Exception e) {
            log.debug("Could not resolve issue ID for key {}: {}", issueKey, e.getMessage());
            return null;
        }
    }

    private String determineDirection(String linkType) {
        // Determine direction based on link type
        return switch (linkType.toLowerCase()) {
            case "blocks", "duplicates", "is duplicated by" -> "OUTWARD";
            case "is blocked by", "relates to" -> "INWARD";
            default -> "OUTWARD";
        };
    }

    private void updateEntityStatus(UUID jobId, String sourceKey, String targetId,
                                    String type, boolean success) {
        if (jobId == null) return;

        try {
            EntityStatus status = entityStatusRepository
                    .findByJobIdAndEntityTypeAndSourceIdentifier(jobId, type, sourceKey)
                    .orElse(EntityStatus.builder()
                            .jobId(jobId)
                            .entityType(type)
                            .sourceIdentifier(sourceKey)
                            .build());

            status.setTargetId(targetId);
            status.setStatus(success ? "SUCCESS" : "FAILED");
            status.setProcessedAt(java.time.LocalDateTime.now());

            entityStatusRepository.save(status);
        } catch (Exception e) {
            log.warn("Failed to update entity status for {}: {}", sourceKey, e.getMessage());
        }
    }

    /**
     * Rollback created issue links on failure.
     */
    public void rollbackCreatedLinks() {
        log.info("Rolling back {} created issue links", createdLinkIds.size());
        for (String linkId : createdLinkIds) {
            try {
                issueLinkServiceClient.deleteIssueLink(linkId);
                log.debug("Rolled back issue link: {}", linkId);
            } catch (Exception e) {
                log.error("Failed to rollback issue link {}: {}", linkId, e.getMessage());
            }
        }
        createdLinkIds.clear();
    }

    /**
     * Clear rollback tracking.
     */
    public void clearRollbackTracking() {
        createdLinkIds.clear();
    }

    public static class IssueLinkPersistResult {
        private boolean success;
        private UUID linkId;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getLinkId() { return linkId; }
        public void setLinkId(UUID linkId) { this.linkId = linkId; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}