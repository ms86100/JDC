package com.jira.sprint.service;

import com.jira.sprint.dto.BulkOperationRequest;
import com.jira.sprint.dto.BulkOperationResponse;
import com.jira.sprint.dto.BulkOperationResult;
import com.jira.sprint.dto.OperationStatus;
import com.jira.sprint.entity.SprintIssue;
import com.jira.sprint.repository.SprintIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkOperationService {

    private final SprintService sprintService;
    private final SprintIssueRepository sprintIssueRepository;
    private final IssueServiceClient issueServiceClient;
    private final MessageSource messageSource;

    private final Map<String, BulkOperationResponse> operations = new java.util.concurrent.ConcurrentHashMap<>();

    @Transactional
    public BulkOperationResponse executeBulkOperation(BulkOperationRequest request) {
        String operationId = UUID.randomUUID().toString();

        BulkOperationResponse response = BulkOperationResponse.builder()
                .operationId(operationId)
                .operationType(request.getOperationType())
                .totalIssues(request.getIssueIds().size())
                .status(OperationStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .results(new ArrayList<>())
                .build();

        operations.put(operationId, response);

        // Process based on operation type
        switch (request.getOperationType()) {
            case UPDATE_STATUS:
                processStatusUpdate(response, request);
                break;
            case UPDATE_FIELDS:
                processFieldUpdates(response, request);
                break;
            case CLONE:
                processClone(response, request);
                break;
            case MOVE_TO_SPRINT:
                processMoveToSprint(response, request);
                break;
            case ADD_LABELS:
                processAddLabels(response, request);
                break;
            case DELETE:
                processDelete(response, request);
                break;
        }

        response.setCompletedAt(LocalDateTime.now());
        updateOperationStatus(response);

        log.info("Bulk operation {} completed: {} issues processed, {} failed",
                operationId, response.getSuccessCount(), response.getFailedCount());

        return response;
    }

    private void processStatusUpdate(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        int failed = 0;
        for (UUID issueId : request.getIssueIds()) {
            try {
                // Fetch the issue to get its projectId (required for status transition)
                IssueServiceClient.IssueData issueData = issueServiceClient.getIssue(issueId);
                if (issueData.getId() == null) {
                    throw new RuntimeException(messageSource.getMessage("error.bulk.issue.not.found", new Object[]{issueId}, Locale.ENGLISH));
                }
                UUID projectId = issueData.getProjectId();
                if (projectId == null) {
                    throw new RuntimeException(messageSource.getMessage("error.bulk.project.unknown", new Object[]{issueId}, Locale.ENGLISH));
                }
                issueServiceClient.updateIssueStatus(issueId, projectId, request.getNewStatus());

                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(issueData.getIssueKey() != null ? issueData.getIssueKey() : issueId.toString())
                        .success(true)
                        .message("Status updated to " + request.getNewStatus())
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                log.warn("Bulk status update failed for issue {}: {}", issueId, e.getMessage());
                response.getResults().add(BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("Failed: " + e.getMessage())
                        .errorCode("STATUS_UPDATE_FAILED")
                        .build());
                failed++;
            }
        }
        response.setSuccessCount(success);
        response.setFailedCount(failed);
    }

    private void processFieldUpdates(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        int failed = 0;

        for (UUID issueId : request.getIssueIds()) {
            try {
                Map<String, Object> fields = new HashMap<>();
                StringBuilder changes = new StringBuilder();

                if (request.getAssigneeId() != null) {
                    fields.put("assigneeId", request.getAssigneeId());
                    changes.append("assignee=").append(request.getAssigneeId()).append(";");
                }
                if (request.getPriority() != null) {
                    fields.put("priorityId", request.getPriority());
                    changes.append("priority=").append(request.getPriority()).append(";");
                }
                if (request.getLabels() != null) {
                    fields.put("labels", request.getLabels().split(","));
                    changes.append("labels=").append(request.getLabels()).append(";");
                }

                if (!fields.isEmpty()) {
                    issueServiceClient.updateIssueFields(issueId, fields);
                }

                // Handle sprint assignment separately via sprint service
                if (request.getSprintId() != null) {
                    UUID targetSprintId = UUID.fromString(request.getSprintId());
                    // Remove from current sprints first
                    List<SprintIssue> currentMemberships = sprintIssueRepository.findByIssueIdAndRemovedAtIsNull(issueId);
                    for (SprintIssue membership : currentMemberships) {
                        sprintService.removeIssueFromSprint(membership.getSprintId(), issueId, "bulk field update");
                    }
                    sprintService.addIssueToSprint(targetSprintId, issueId);
                    changes.append("sprint=").append(request.getSprintId()).append(";");
                }

                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(true)
                        .message("Fields updated: " + changes)
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                log.warn("Bulk field update failed for issue {}: {}", issueId, e.getMessage());
                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("Update failed: " + e.getMessage())
                        .errorCode("UPDATE_FAILED")
                        .build();
                response.getResults().add(result);
                failed++;
            }
        }

        response.setSuccessCount(success);
        response.setFailedCount(failed);
    }

    private void processClone(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        int failed = 0;
        boolean keepAttachments = Boolean.TRUE.equals(request.getKeepAttachments());

        for (UUID issueId : request.getIssueIds()) {
            try {
                String cloneKey = issueServiceClient.cloneIssue(
                        issueId, request.getTargetProjectId(), keepAttachments);

                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(cloneKey)
                        .success(true)
                        .message("Cloned " + issueId
                                + (request.getTargetProjectId() != null ? " to project " + request.getTargetProjectId() : "")
                                + " -> " + cloneKey)
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                log.warn("Bulk clone failed for issue {}: {}", issueId, e.getMessage());
                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("Clone failed: " + e.getMessage())
                        .errorCode("CLONE_FAILED")
                        .build();
                response.getResults().add(result);
                failed++;
            }
        }

        response.setSuccessCount(success);
        response.setFailedCount(failed);
    }

    private void processMoveToSprint(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        int failed = 0;

        if (request.getSprintId() == null) {
            for (UUID issueId : request.getIssueIds()) {
                response.getResults().add(BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("Target sprintId is required for MOVE_TO_SPRINT")
                        .errorCode("MISSING_SPRINT_ID")
                        .build());
                failed++;
            }
            response.setSuccessCount(0);
            response.setFailedCount(failed);
            return;
        }

        UUID targetSprintId = UUID.fromString(request.getSprintId());

        for (UUID issueId : request.getIssueIds()) {
            try {
                // Remove issue from any current sprint(s)
                List<SprintIssue> currentMemberships = sprintIssueRepository.findByIssueIdAndRemovedAtIsNull(issueId);
                for (SprintIssue membership : currentMemberships) {
                    if (!membership.getSprintId().equals(targetSprintId)) {
                        sprintService.removeIssueFromSprint(membership.getSprintId(), issueId, "bulk move to sprint " + targetSprintId);
                    }
                }

                // Add to target sprint (addIssueToSprint handles duplicate check)
                try {
                    sprintService.addIssueToSprint(targetSprintId, issueId);
                } catch (IllegalArgumentException e) {
                    // Already in target sprint -- that's fine for a move operation
                    if (!e.getMessage().contains("already in sprint")) {
                        throw e;
                    }
                }

                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(true)
                        .message("Moved to sprint " + targetSprintId)
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                log.warn("Bulk move-to-sprint failed for issue {}: {}", issueId, e.getMessage());
                response.getResults().add(BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("Move failed: " + e.getMessage())
                        .errorCode("MOVE_FAILED")
                        .build());
                failed++;
            }
        }
        response.setSuccessCount(success);
        response.setFailedCount(failed);
    }

    private void processAddLabels(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        int failed = 0;

        if (request.getLabels() == null || request.getLabels().isBlank()) {
            for (UUID issueId : request.getIssueIds()) {
                response.getResults().add(BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("No labels provided")
                        .errorCode("MISSING_LABELS")
                        .build());
                failed++;
            }
            response.setSuccessCount(0);
            response.setFailedCount(failed);
            return;
        }

        String[] newLabels = request.getLabels().split(",");

        for (UUID issueId : request.getIssueIds()) {
            try {
                Map<String, Object> fields = new HashMap<>();
                fields.put("labels", newLabels);
                issueServiceClient.updateIssueFields(issueId, fields);

                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(true)
                        .message("Labels updated: " + request.getLabels())
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                log.warn("Bulk add-labels failed for issue {}: {}", issueId, e.getMessage());
                response.getResults().add(BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("Label update failed: " + e.getMessage())
                        .errorCode("LABEL_UPDATE_FAILED")
                        .build());
                failed++;
            }
        }
        response.setSuccessCount(success);
        response.setFailedCount(failed);
    }

    private void processDelete(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        int failed = 0;

        for (UUID issueId : request.getIssueIds()) {
            try {
                // Remove from any sprints first
                List<SprintIssue> memberships = sprintIssueRepository.findByIssueIdAndRemovedAtIsNull(issueId);
                for (SprintIssue membership : memberships) {
                    sprintService.removeIssueFromSprint(membership.getSprintId(), issueId, "bulk delete");
                }

                // Delete the issue via the issue service
                issueServiceClient.deleteIssue(issueId);

                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(true)
                        .message("Deleted")
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                log.warn("Bulk delete failed for issue {}: {}", issueId, e.getMessage());
                response.getResults().add(BulkOperationResult.builder()
                        .issueKey(issueId.toString())
                        .success(false)
                        .message("Delete failed: " + e.getMessage())
                        .errorCode("DELETE_FAILED")
                        .build());
                failed++;
            }
        }
        response.setSuccessCount(success);
        response.setFailedCount(failed);
    }

    private void updateOperationStatus(BulkOperationResponse response) {
        if (response.getFailedCount() == 0) {
            response.setStatus(OperationStatus.COMPLETED);
        } else if (response.getSuccessCount() == 0) {
            response.setStatus(OperationStatus.FAILED);
        } else {
            response.setStatus(OperationStatus.PARTIAL_SUCCESS);
        }
    }

    public BulkOperationResponse getOperationStatus(String operationId) {
        return operations.get(operationId);
    }

    public List<BulkOperationResponse> getRecentOperations() {
        return operations.values().stream()
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .limit(10)
                .collect(Collectors.toList());
    }
}