package com.jira.issue.service;

import com.jira.issue.dto.WorklogRequest;
import com.jira.issue.dto.WorklogResponse;
import com.jira.issue.entity.Worklog;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueRepository;
import com.jira.issue.repository.WorklogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorklogService {

    private final WorklogRepository worklogRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public WorklogResponse createWorklog(WorklogRequest request) {
        if (!issueRepository.existsById(request.getIssueId())) {
            throw new ResourceNotFoundException("Issue not found: " + request.getIssueId());
        }

        Worklog worklog = Worklog.builder()
                .issueId(request.getIssueId())
                .authorId(request.getAuthorId())
                .startedAt(request.getStartedAt() != null ? request.getStartedAt() : LocalDateTime.now())
                .timeSpentSeconds(request.getTimeSpentSeconds())
                .workDescription(request.getWorkDescription())
                .build();

        worklog = worklogRepository.save(worklog);
        log.info("Created worklog {} for issue {}", worklog.getId(), request.getIssueId());

        return toResponse(worklog);
    }

    @Transactional(readOnly = true)
    public List<WorklogResponse> getWorklogsByIssue(UUID issueId) {
        return worklogRepository.findByIssueIdOrderByStartedAtDesc(issueId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorklogResponse getWorklog(UUID worklogId) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new ResourceNotFoundException("Worklog not found: " + worklogId));
        return toResponse(worklog);
    }

    @Transactional
    public WorklogResponse updateWorklog(UUID worklogId, WorklogRequest request) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new ResourceNotFoundException("Worklog not found: " + worklogId));

        worklog.setTimeSpentSeconds(request.getTimeSpentSeconds());
        worklog.setWorkDescription(request.getWorkDescription());
        if (request.getStartedAt() != null) {
            worklog.setStartedAt(request.getStartedAt());
        }

        worklog = worklogRepository.save(worklog);
        log.info("Updated worklog {}", worklogId);

        return toResponse(worklog);
    }

    @Transactional
    public void deleteWorklog(UUID worklogId) {
        if (!worklogRepository.existsById(worklogId)) {
            throw new ResourceNotFoundException("Worklog not found: " + worklogId);
        }
        worklogRepository.deleteById(worklogId);
        log.info("Deleted worklog {}", worklogId);
    }

    @Transactional(readOnly = true)
    public Long getTotalTimeWorked(UUID issueId) {
        Long total = worklogRepository.getTotalTimeSpent(issueId);
        return total != null ? total : 0L;
    }

    private WorklogResponse toResponse(Worklog worklog) {
        return WorklogResponse.builder()
                .id(worklog.getId())
                .issueId(worklog.getIssueId())
                .authorId(worklog.getAuthorId())
                .timeSpentSeconds(worklog.getTimeSpentSeconds())
                .workDescription(worklog.getWorkDescription())
                .startedAt(worklog.getStartedAt())
                .createdAt(worklog.getCreatedAt())
                .build();
    }
}