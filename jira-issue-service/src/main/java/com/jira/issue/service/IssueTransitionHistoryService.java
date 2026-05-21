package com.jira.issue.service;

import com.jira.issue.dto.IssueTransitionHistoryResponse;
import com.jira.issue.dto.RecordIssueTransitionRequest;
import com.jira.issue.entity.IssueStatusHistory;
import com.jira.issue.entity.IssueTransitionHistory;
import com.jira.issue.repository.IssueStatusHistoryRepository;
import com.jira.issue.repository.IssueTransitionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueTransitionHistoryService {

    private final IssueTransitionHistoryRepository transitionHistoryRepository;
    private final IssueStatusHistoryRepository statusHistoryRepository;

    @Transactional
    public IssueTransitionHistoryResponse record(UUID issueId, RecordIssueTransitionRequest request) {
        boolean success = request.getSuccess() == null || request.getSuccess();
        OffsetDateTime now = OffsetDateTime.now();

        IssueTransitionHistory row = transitionHistoryRepository.save(IssueTransitionHistory.builder()
                .issueId(issueId)
                .projectId(request.getProjectId())
                .workflowId(request.getWorkflowId())
                .transitionId(request.getTransitionId())
                .transitionName(request.getTransitionName())
                .fromStatusId(request.getFromStatusId())
                .toStatusId(request.getToStatusId())
                .userId(request.getUserId())
                .comment(request.getComment())
                .success(success)
                .errorMessage(request.getErrorMessage())
                .executedAt(now)
                .build());

        if (success && request.getToStatusId() != null) {
            statusHistoryRepository.save(IssueStatusHistory.builder()
                    .issueId(issueId)
                    .fromStatusId(request.getFromStatusId())
                    .toStatusId(request.getToStatusId())
                    .transitionId(request.getTransitionId())
                    .userId(request.getUserId())
                    .changedAt(now)
                    .build());
        }

        return map(row);
    }

    @Transactional(readOnly = true)
    public List<IssueTransitionHistoryResponse> listByIssue(UUID issueId) {
        return transitionHistoryRepository.findByIssueIdOrderByExecutedAtDesc(issueId).stream()
                .map(this::map)
                .toList();
    }

    private IssueTransitionHistoryResponse map(IssueTransitionHistory row) {
        return IssueTransitionHistoryResponse.builder()
                .id(row.getId())
                .issueId(row.getIssueId())
                .projectId(row.getProjectId())
                .workflowId(row.getWorkflowId())
                .transitionId(row.getTransitionId())
                .transitionName(row.getTransitionName())
                .fromStatusId(row.getFromStatusId())
                .toStatusId(row.getToStatusId())
                .userId(row.getUserId())
                .comment(row.getComment())
                .success(row.getSuccess())
                .errorMessage(row.getErrorMessage())
                .executedAt(row.getExecutedAt())
                .build();
    }
}
