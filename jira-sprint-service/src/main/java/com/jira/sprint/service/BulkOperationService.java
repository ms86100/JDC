package com.jira.sprint.service;

import com.jira.sprint.dto.BulkOperationRequest;
import com.jira.sprint.dto.BulkOperationResponse;
import com.jira.sprint.dto.BulkOperationResult;
import com.jira.sprint.dto.OperationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkOperationService {

    private final Map<String, BulkOperationResponse> operations = new HashMap<>();

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
        for (UUID issueId : request.getIssueIds()) {
            // Mock status update
            BulkOperationResult result = BulkOperationResult.builder()
                    .issueKey("JRA-" + issueId.hashCode() % 1000)
                    .success(true)
                    .message("Status updated to " + request.getNewStatus())
                    .build();
            response.getResults().add(result);
            success++;
        }
        response.setSuccessCount(success);
        response.setFailedCount(0);
    }

    private void processFieldUpdates(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        int failed = 0;

        for (UUID issueId : request.getIssueIds()) {
            try {
                StringBuilder changes = new StringBuilder();
                if (request.getAssigneeId() != null) {
                    changes.append("assignee=").append(request.getAssigneeId()).append(";");
                }
                if (request.getPriority() != null) {
                    changes.append("priority=").append(request.getPriority()).append(";");
                }
                if (request.getSprintId() != null) {
                    changes.append("sprint=").append(request.getSprintId()).append(";");
                }

                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey("JRA-" + Math.abs(issueId.hashCode() % 10000))
                        .success(true)
                        .message("Fields updated: " + changes)
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey("JRA-" + Math.abs(issueId.hashCode() % 10000))
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

        for (UUID issueId : request.getIssueIds()) {
            try {
                String newKey = "JRA-" + (1000 + new Random().nextInt());
                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey(newKey)
                        .success(true)
                        .message("Cloned to " + newKey + (request.getTargetProjectId() != null ? " in project" : ""))
                        .build();
                response.getResults().add(result);
                success++;
            } catch (Exception e) {
                BulkOperationResult result = BulkOperationResult.builder()
                        .issueKey("Unknown")
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
        for (UUID issueId : request.getIssueIds()) {
            BulkOperationResult result = BulkOperationResult.builder()
                    .issueKey("JRA-" + Math.abs(issueId.hashCode() % 10000))
                    .success(true)
                    .message("Moved to sprint")
                    .build();
            response.getResults().add(result);
            success++;
        }
        response.setSuccessCount(success);
        response.setFailedCount(0);
    }

    private void processAddLabels(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        for (UUID issueId : request.getIssueIds()) {
            BulkOperationResult result = BulkOperationResult.builder()
                    .issueKey("JRA-" + Math.abs(issueId.hashCode() % 10000))
                    .success(true)
                    .message("Labels added: " + request.getLabels())
                    .build();
            response.getResults().add(result);
            success++;
        }
        response.setSuccessCount(success);
        response.setFailedCount(0);
    }

    private void processDelete(BulkOperationResponse response, BulkOperationRequest request) {
        int success = 0;
        for (UUID issueId : request.getIssueIds()) {
            BulkOperationResult result = BulkOperationResult.builder()
                    .issueKey("JRA-" + Math.abs(issueId.hashCode() % 10000))
                    .success(true)
                    .message("Deleted")
                    .build();
            response.getResults().add(result);
            success++;
        }
        response.setSuccessCount(success);
        response.setFailedCount(0);
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