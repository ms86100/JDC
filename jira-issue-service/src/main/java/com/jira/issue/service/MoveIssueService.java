package com.jira.issue.service;

import com.jira.issue.dto.IssueResponse;
import com.jira.issue.entity.Issue;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for moving issues between projects
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoveIssueService {

    private final IssueRepository issueRepository;

    @Transactional
    public IssueResponse moveIssue(UUID issueId, UUID targetProjectId, UUID userId) {
        log.info("Moving issue {} to project {} by user {}", issueId, targetProjectId, userId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        // Get new project key for new issue key
        String newIssueKey = "MOVED-" + System.currentTimeMillis() % 10000;

        // Update issue with new project
        issue.setProjectId(targetProjectId);
        issue.setIssueKey(newIssueKey);

        issue = issueRepository.save(issue);
        log.info("Issue moved successfully: {} -> project {}", issue.getIssueKey(), targetProjectId);

        return mapToResponse(issue);
    }

    private IssueResponse mapToResponse(Issue issue) {
        return IssueResponse.builder()
                .id(issue.getId())
                .projectId(issue.getProjectId())
                .issueKey(issue.getIssueKey())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .statusId(issue.getStatus() != null ? issue.getStatus().getId() : null)
                .priorityId(issue.getPriority() != null ? issue.getPriority().getId() : null)
                .issueTypeId(issue.getIssueType() != null ? issue.getIssueType().getId() : null)
                .build();
    }
}