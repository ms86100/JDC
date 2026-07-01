package com.jira.workflow.service;

import com.jira.workflow.dto.*;
import com.jira.workflow.entity.*;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowMigrationService {

    private final WorkflowMigrationRepository workflowMigrationRepository;
    private final WorkflowMigrationIssueRepository workflowMigrationIssueRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowAuditLogRepository workflowAuditLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ISSUE_SERVICE_URL = "http://jira-issue-service:8084";

    @Transactional
    public WorkflowMigrationResponse createMigration(UUID workflowId, UUID oldStatusId,
                                                      UUID newStatusId, String migrationType, UUID userId) {
        log.info("Creating migration for workflow {}: {} -> {}", workflowId, oldStatusId, newStatusId);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        int issueCount = countIssuesInStatus(workflowId, oldStatusId);

        WorkflowMigration migration = WorkflowMigration.builder()
                .workflowId(workflowId)
                .oldStatusId(oldStatusId)
                .newStatusId(newStatusId)
                .migrationType(migrationType != null ? migrationType : WorkflowMigration.TYPE_STATUS_CHANGE)
                .issueCount(issueCount)
                .migratedCount(0)
                .migrationStatus(WorkflowMigration.STATUS_PENDING)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        migration = workflowMigrationRepository.save(migration);
        log.info("Migration created: {}", migration.getId());

        return mapToResponse(migration);
    }

    @Transactional
    public WorkflowMigrationResponse startMigration(UUID migrationId) {
        log.info("Starting migration: {}", migrationId);

        WorkflowMigration migration = workflowMigrationRepository.findById(migrationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowMigration", "id", migrationId));

        migration.setMigrationStatus(WorkflowMigration.STATUS_IN_PROGRESS);
        migration.setStartedAt(LocalDateTime.now());
        migration = workflowMigrationRepository.save(migration);

        return mapToResponse(migration);
    }

    @Transactional
    public WorkflowMigrationResponse executeMigration(UUID migrationId) {
        log.info("Executing migration: {}", migrationId);

        WorkflowMigration migration = workflowMigrationRepository.findById(migrationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowMigration", "id", migrationId));

        List<UUID> issues = getIssuesInStatus(migration.getWorkflowId(), migration.getOldStatusId());

        int migrated = 0;
        int failed = 0;

        for (UUID issueId : issues) {
            try {
                boolean success = migrateIssue(issueId, migration.getOldStatusId(), migration.getNewStatusId());
                if (success) {
                    migrated++;
                    createMigrationIssue(migration.getId(), issueId, migration.getOldStatusId(),
                            migration.getNewStatusId(), WorkflowMigrationIssue.STATUS_MIGRATED);
                } else {
                    failed++;
                    createMigrationIssue(migration.getId(), issueId, migration.getOldStatusId(),
                            migration.getNewStatusId(), WorkflowMigrationIssue.STATUS_FAILED);
                }
            } catch (Exception e) {
                log.error("Failed to migrate issue {}: {}", issueId, e.getMessage());
                failed++;
                createMigrationIssue(migration.getId(), issueId, migration.getOldStatusId(),
                        migration.getNewStatusId(), WorkflowMigrationIssue.STATUS_FAILED, e.getMessage());
            }
        }

        migration.setMigratedCount(migrated);
        migration.setMigrationStatus(failed == 0 ? WorkflowMigration.STATUS_COMPLETED : WorkflowMigration.STATUS_FAILED);
        migration.setCompletedAt(LocalDateTime.now());
        if (failed > 0) {
            migration.setErrorMessage(failed + " issues failed to migrate");
        }
        workflowMigrationRepository.save(migration);

        log.info("Migration {} completed: {} migrated, {} failed", migrationId, migrated, failed);
        return mapToResponse(migration);
    }

    @Transactional(readOnly = true)
    public MigrationPreviewResponse previewMigration(UUID workflowId, UUID oldStatusId, UUID newStatusId) {
        log.info("Previewing migration for workflow {}: {} -> {}", workflowId, oldStatusId, newStatusId);

        List<UUID> issues = getIssuesInStatus(workflowId, oldStatusId);
        List<IssuePreview> issuePreviews = new ArrayList<>();

        for (UUID issueId : issues) {
            Map<String, Object> issueData = fetchIssueData(issueId);
            String issueKey = (String) issueData.getOrDefault("issueKey", "UNKNOWN-" + issueId);
            String summary = (String) issueData.getOrDefault("summary", "");
            Object statusIdObj = issueData.get("statusId");
            UUID currentStatusId = statusIdObj != null ? UUID.fromString(statusIdObj.toString()) : null;

            issuePreviews.add(IssuePreview.builder()
                    .issueId(issueId)
                    .issueKey(issueKey)
                    .summary(summary)
                    .currentStatusId(currentStatusId)
                    .build());
        }

        return MigrationPreviewResponse.builder()
                .workflowId(workflowId)
                .oldStatusId(oldStatusId)
                .newStatusId(newStatusId)
                .issueCount(issues.size())
                .issues(issuePreviews)
                .build();
    }

    @Transactional(readOnly = true)
    public WorkflowMigrationResponse getMigration(UUID migrationId) {
        WorkflowMigration migration = workflowMigrationRepository.findById(migrationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowMigration", "id", migrationId));
        return mapToResponse(migration);
    }

    @Transactional(readOnly = true)
    public List<WorkflowMigrationResponse> getMigrationsForWorkflow(UUID workflowId) {
        return workflowMigrationRepository.findByWorkflowId(workflowId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WorkflowMigrationResponse> getMigrationsForWorkflow(UUID workflowId, Pageable pageable) {
        return workflowMigrationRepository.findByWorkflowId(workflowId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowMigrationIssueResponse> getMigrationIssues(UUID migrationId, Pageable pageable) {
        return workflowMigrationIssueRepository.findByMigrationId(migrationId, pageable)
                .map(this::mapIssueToResponse);
    }

    @Transactional
    public WorkflowMigrationResponse cancelMigration(UUID migrationId) {
        log.info("Cancelling migration: {}", migrationId);

        WorkflowMigration migration = workflowMigrationRepository.findById(migrationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowMigration", "id", migrationId));

        if (WorkflowMigration.STATUS_COMPLETED.equals(migration.getMigrationStatus())) {
            throw new IllegalStateException("Cannot cancel completed migration");
        }

        migration.setMigrationStatus(WorkflowMigration.STATUS_FAILED);
        migration.setErrorMessage("Cancelled by user");
        migration.setCompletedAt(LocalDateTime.now());
        workflowMigrationRepository.save(migration);

        return mapToResponse(migration);
    }

    @Transactional
    public WorkflowMigrationResponse retryFailedIssues(UUID migrationId) {
        log.info("Retrying failed issues for migration: {}", migrationId);

        WorkflowMigration migration = workflowMigrationRepository.findById(migrationId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowMigration", "id", migrationId));

        List<WorkflowMigrationIssue> failedIssues = workflowMigrationIssueRepository
                .findByMigrationIdAndMigrationStatus(migrationId, WorkflowMigrationIssue.STATUS_FAILED);

        int migrated = 0;
        for (WorkflowMigrationIssue issue : failedIssues) {
            try {
                boolean success = migrateIssue(issue.getIssueId(), migration.getOldStatusId(), migration.getNewStatusId());
                if (success) {
                    issue.setMigrationStatus(WorkflowMigrationIssue.STATUS_MIGRATED);
                    issue.setProcessedAt(LocalDateTime.now());
                    migrated++;
                } else {
                    issue.setMigrationStatus(WorkflowMigrationIssue.STATUS_FAILED);
                }
            } catch (Exception e) {
                issue.setMigrationStatus(WorkflowMigrationIssue.STATUS_FAILED);
                issue.setErrorMessage(e.getMessage());
            }
            workflowMigrationIssueRepository.save(issue);
        }

        migration.setMigratedCount(migration.getMigratedCount() + migrated);
        long stillFailed = workflowMigrationIssueRepository
                .countByMigrationIdAndMigrationStatus(migrationId, WorkflowMigrationIssue.STATUS_FAILED);
        migration.setMigrationStatus(stillFailed == 0 ? WorkflowMigration.STATUS_COMPLETED : WorkflowMigration.STATUS_FAILED);
        workflowMigrationRepository.save(migration);

        return mapToResponse(migration);
    }

    private int countIssuesInStatus(UUID workflowId, UUID statusId) {
        try {
            String url = ISSUE_SERVICE_URL + "/api/issues/count?workflowId=" + workflowId + "&statusId=" + statusId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("count")) {
                return ((Number) response.get("count")).intValue();
            }
        } catch (Exception e) {
            log.warn("Could not fetch issue count: {}", e.getMessage());
        }
        return 0;
    }

    private List<UUID> getIssuesInStatus(UUID workflowId, UUID statusId) {
        try {
            String url = ISSUE_SERVICE_URL + "/api/issues?workflowId=" + workflowId + "&statusId=" + statusId;
            List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);
            if (response != null) {
                return response.stream()
                        .map(m -> UUID.fromString(m.get("id").toString()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Could not fetch issues: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private Map<String, Object> fetchIssueData(UUID issueId) {
        try {
            String url = ISSUE_SERVICE_URL + "/api/issues/" + issueId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response : new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to fetch issue data: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private boolean migrateIssue(UUID issueId, UUID oldStatusId, UUID newStatusId) {
        try {
            String url = ISSUE_SERVICE_URL + "/api/issues/" + issueId + "/status";
            Map<String, Object> update = new HashMap<>();
            update.put("statusId", newStatusId.toString());
            restTemplate.put(url, update);
            return true;
        } catch (Exception e) {
            log.error("Failed to migrate issue {}: {}", issueId, e.getMessage());
            return false;
        }
    }

    private void createMigrationIssue(UUID migrationId, UUID issueId, UUID oldStatusId,
                                      UUID newStatusId, String status) {
        createMigrationIssue(migrationId, issueId, oldStatusId, newStatusId, status, null);
    }

    private void createMigrationIssue(UUID migrationId, UUID issueId, UUID oldStatusId,
                                      UUID newStatusId, String status, String errorMessage) {
        WorkflowMigrationIssue issue = WorkflowMigrationIssue.builder()
                .migrationId(migrationId)
                .issueId(issueId)
                .oldStatusId(oldStatusId)
                .newStatusId(newStatusId)
                .migrationStatus(status)
                .processedAt(LocalDateTime.now())
                .errorMessage(errorMessage)
                .build();
        workflowMigrationIssueRepository.save(issue);
    }

    private WorkflowMigrationResponse mapToResponse(WorkflowMigration migration) {
        List<WorkflowMigrationIssue> issues = workflowMigrationIssueRepository.findByMigrationId(migration.getId());

        List<WorkflowMigrationIssueResponse> issueResponses = issues.stream()
                .map(this::mapIssueToResponse)
                .collect(Collectors.toList());

        return WorkflowMigrationResponse.builder()
                .id(migration.getId())
                .workflowId(migration.getWorkflowId())
                .workflowVersionId(migration.getWorkflowVersionId())
                .oldStatusId(migration.getOldStatusId())
                .newStatusId(migration.getNewStatusId())
                .migrationType(migration.getMigrationType())
                .issueCount(migration.getIssueCount())
                .migratedCount(migration.getMigratedCount())
                .migrationStatus(migration.getMigrationStatus())
                .createdBy(migration.getCreatedBy())
                .createdAt(migration.getCreatedAt())
                .startedAt(migration.getStartedAt())
                .completedAt(migration.getCompletedAt())
                .errorMessage(migration.getErrorMessage())
                .issues(issueResponses)
                .build();
    }

    private WorkflowMigrationIssueResponse mapIssueToResponse(WorkflowMigrationIssue issue) {
        Map<String, Object> issueData = fetchIssueData(issue.getIssueId());
        String issueKey = (String) issueData.getOrDefault("issueKey", "UNKNOWN-" + issue.getIssueId());

        return WorkflowMigrationIssueResponse.builder()
                .id(issue.getId())
                .issueId(issue.getIssueId())
                .issueKey(issueKey)
                .oldStatusId(issue.getOldStatusId())
                .newStatusId(issue.getNewStatusId())
                .migrationStatus(issue.getMigrationStatus())
                .processedAt(issue.getProcessedAt())
                .errorMessage(issue.getErrorMessage())
                .build();
    }
}