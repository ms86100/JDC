package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.dto.bulk.*;
import com.jira.issue.entity.Issue;
import com.jira.issue.entity.IssuePriority;
import com.jira.issue.entity.IssueStatus;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.dto.bulk.BulkOperationType;
import com.jira.issue.security.ProjectPermissionGuard;
import com.jira.issue.repository.IssuePriorityRepository;
import com.jira.issue.repository.IssueRepository;
import com.jira.issue.repository.IssueStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkIssueOperationService {

    private final IssueService issueService;
    private final IssueRepository issueRepository;
    private final IssuePriorityRepository issuePriorityRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final LabelService labelService;
    private final WorkflowTransitionClient workflowTransitionClient;
    private final AuditIntegrationClient auditIntegrationClient;
    private final ProjectPermissionGuard permissionGuard;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkOperationResponse execute(BulkOperationRequest request, UUID userId) {
        UUID projectId = request.getProjectId();
        if (projectId == null && !request.getIssueIds().isEmpty()) {
            projectId = issueRepository.findById(request.getIssueIds().get(0))
                    .map(Issue::getProjectId)
                    .orElse(null);
        }
        if (projectId != null) {
            assertBulkPermission(request.getOperationType(), userId, projectId);
        } else {
            permissionGuard.requireUser(userId);
        }
        String operationId = UUID.randomUUID().toString();
        BulkOperationResponse response = BulkOperationResponse.builder()
                .operationId(operationId)
                .operationType(request.getOperationType())
                .totalIssues(request.getIssueIds().size())
                .status("IN_PROGRESS")
                .startedAt(LocalDateTime.now())
                .results(new ArrayList<>())
                .build();

        int success = 0;
        int failed = 0;

        for (UUID issueId : request.getIssueIds()) {
            BulkOperationResultItem item = processOne(request, issueId, userId);
            response.getResults().add(item);
            if (item.isSuccess()) {
                success++;
            } else {
                failed++;
            }
        }

        response.setSuccessCount(success);
        response.setFailedCount(failed);
        response.setStatus(failed == 0 ? "COMPLETED" : (success == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        response.setCompletedAt(LocalDateTime.now());

        Map<String, Object> auditChanges = new HashMap<>();
        auditChanges.put("operationType", request.getOperationType().name());
        auditChanges.put("total", request.getIssueIds().size());
        auditChanges.put("succeeded", success);
        auditChanges.put("failed", failed);
        auditChanges.put("issueIds", request.getIssueIds().stream().map(UUID::toString).toList());
        auditIntegrationClient.logIssueEvent(
                userId,
                request.getIssueIds().isEmpty() ? UUID.randomUUID() : request.getIssueIds().get(0),
                "BULK_" + request.getOperationType().name(),
                auditChanges);

        log.info("Bulk {} completed: {} ok, {} failed", request.getOperationType(), success, failed);
        return response;
    }

    private void assertBulkPermission(
            BulkOperationType operationType,
            UUID userId,
            UUID projectId) {
        switch (operationType) {
            case DELETE -> permissionGuard.requirePermission(userId, projectId, "DELETE_ISSUES");
            case UPDATE_STATUS -> permissionGuard.requirePermission(userId, projectId, "RESOLVE_ISSUES");
            case CLONE -> permissionGuard.requirePermission(userId, projectId, "CREATE_ISSUES");
            case UPDATE_FIELDS, ADD_LABELS, MOVE_TO_SPRINT ->
                    permissionGuard.requirePermission(userId, projectId, "EDIT_ISSUES");
        }
    }

    private BulkOperationResultItem processOne(BulkOperationRequest request, UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId).orElse(null);
        String issueKey = issue != null ? issue.getIssueKey() : issueId.toString();

        try {
            return switch (request.getOperationType()) {
                case UPDATE_STATUS -> bulkStatus(request, issue, userId);
                case UPDATE_FIELDS -> bulkFields(request, issue, userId);
                case ADD_LABELS -> bulkLabels(request, issue, userId);
                case CLONE -> bulkClone(request, issue, userId);
                case DELETE -> bulkDelete(issue, userId);
                case MOVE_TO_SPRINT -> BulkOperationResultItem.builder()
                        .issueId(issueId)
                        .issueKey(issueKey)
                        .success(false)
                        .message("Move to sprint: assign sprint on board view (sprint API pending)")
                        .errorCode("NOT_IMPLEMENTED")
                        .build();
            };
        } catch (Exception e) {
            log.warn("Bulk item failed for {}: {}", issueKey, e.getMessage());
            return BulkOperationResultItem.builder()
                    .issueId(issueId)
                    .issueKey(issueKey)
                    .success(false)
                    .message(e.getMessage())
                    .errorCode("BULK_ITEM_FAILED")
                    .build();
        }
    }

    private BulkOperationResultItem bulkStatus(BulkOperationRequest request, Issue issue, UUID userId) {
        if (issue == null) {
            throw new ResourceNotFoundException("Issue", "id", request.getIssueIds());
        }
        UUID projectId = request.getProjectId() != null ? request.getProjectId() : issue.getProjectId();

        if (request.getTransitionId() != null) {
            workflowTransitionClient.executeTransition(
                    issue.getId(), projectId, userId,
                    request.getTransitionId(), null, null, null, null);
            return BulkOperationResultItem.builder()
                    .issueId(issue.getId())
                    .issueKey(issue.getIssueKey())
                    .success(true)
                    .message("Transition executed")
                    .build();
        }

        UUID statusId = request.getNewStatus() != null
                ? resolveStatusId(request.getNewStatus())
                : null;
        if (statusId == null) {
            throw new IllegalArgumentException("newStatus or transitionId is required for UPDATE_STATUS");
        }

        UpdateIssueStatusRequest statusReq = UpdateIssueStatusRequest.builder()
                .statusId(statusId)
                .build();
        issueService.updateIssueStatus(issue.getId(), statusReq, projectId, userId);

        return BulkOperationResultItem.builder()
                .issueId(issue.getId())
                .issueKey(issue.getIssueKey())
                .success(true)
                .message("Status updated")
                .build();
    }

    private BulkOperationResultItem bulkFields(BulkOperationRequest request, Issue issue, UUID userId) {
        if (issue == null) {
            throw new ResourceNotFoundException("Issue", "id", request.getIssueIds());
        }
        UpdateIssueRequest update = UpdateIssueRequest.builder().build();
        if (request.getAssigneeId() != null) {
            update.setAssigneeId(request.getAssigneeId());
        }
        UUID priorityId = request.getPriorityId();
        if (priorityId == null && request.getPriority() != null) {
            priorityId = issuePriorityRepository.findAll().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(request.getPriority()))
                    .map(IssuePriority::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (priorityId != null) {
            update.setPriorityId(priorityId);
        }
        issueService.updateIssue(issue.getId(), update);
        return BulkOperationResultItem.builder()
                .issueId(issue.getId())
                .issueKey(issue.getIssueKey())
                .success(true)
                .message("Fields updated")
                .build();
    }

    private BulkOperationResultItem bulkLabels(BulkOperationRequest request, Issue issue, UUID userId) {
        if (issue == null) {
            throw new ResourceNotFoundException("Issue", "id", request.getIssueIds());
        }
        if (request.getLabels() == null || request.getLabels().isBlank()) {
            throw new IllegalArgumentException("labels required for ADD_LABELS");
        }
        for (String raw : request.getLabels().split("[,;]")) {
            String name = raw.trim().toLowerCase();
            if (!name.isEmpty()) {
                labelService.addLabel(LabelRequest.builder()
                        .issueId(issue.getId())
                        .name(name)
                        .build());
            }
        }
        return BulkOperationResultItem.builder()
                .issueId(issue.getId())
                .issueKey(issue.getIssueKey())
                .success(true)
                .message("Labels added")
                .build();
    }

    private BulkOperationResultItem bulkClone(BulkOperationRequest request, Issue issue, UUID userId) {
        if (issue == null) {
            throw new ResourceNotFoundException("Issue", "id", request.getIssueIds());
        }
        IssueResponse cloned = issueService.cloneIssue(
                issue.getId(),
                request.getTargetProjectId() != null ? request.getTargetProjectId() : issue.getProjectId(),
                userId);
        return BulkOperationResultItem.builder()
                .issueId(issue.getId())
                .issueKey(issue.getIssueKey())
                .success(true)
                .message("Cloned to " + cloned.getIssueKey())
                .build();
    }

    private BulkOperationResultItem bulkDelete(Issue issue, UUID userId) {
        if (issue == null) {
            throw new ResourceNotFoundException("Issue", "id", null);
        }
        issueService.deleteIssue(issue.getId());
        auditIntegrationClient.logIssueEvent(userId, issue.getId(), "ISSUE_DELETED", Map.of("issueKey", issue.getIssueKey()));
        return BulkOperationResultItem.builder()
                .issueId(issue.getId())
                .issueKey(issue.getIssueKey())
                .success(true)
                .message("Deleted")
                .build();
    }

    private UUID resolveStatusId(String statusNameOrId) {
        try {
            return UUID.fromString(statusNameOrId);
        } catch (IllegalArgumentException ignored) {
            return issueStatusRepository.findAll().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(statusNameOrId.trim()))
                    .map(IssueStatus::getId)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("IssueStatus", "name", statusNameOrId));
        }
    }
}
