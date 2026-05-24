package com.jira.issue.service;

import com.jira.issue.dto.CloneIssueResponse;
import com.jira.issue.dto.IssueResponse;
import com.jira.issue.entity.Issue;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for cloning issues
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloneIssueService {

    private final IssueRepository issueRepository;

    @Transactional
    public CloneIssueResponse cloneIssue(UUID issueId, UUID userId, boolean includeComments, boolean includeAttachments) {
        log.info("Cloning issue {} by user {}", issueId, userId);

        Issue original = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        // Generate new issue key with "-CLONE" suffix
        String newIssueKey = original.getIssueKey() + "-CLONE";

        Issue clone = Issue.builder()
                .projectId(original.getProjectId())
                .issueKey(newIssueKey)
                .title(original.getTitle() + " (Copy)")
                .description(original.getDescription())
                .status(original.getStatus())
                .priority(original.getPriority())
                .issueType(original.getIssueType())
                .reporterId(userId)
                .assigneeId(original.getAssigneeId())
                .resolutionId(original.getResolutionId())
                .labels(original.getLabels())
                .originalEstimate(original.getOriginalEstimate())
                .fixVersions(original.getFixVersions())
                .affectsVersions(original.getAffectsVersions())
                .storyPoints(original.getStoryPoints())
                .parentIssueId(original.getParentIssueId())
                .epicId(original.getEpicId())
                .build();

        clone = issueRepository.save(clone);
        log.info("Issue cloned successfully: {} -> {}", original.getIssueKey(), clone.getIssueKey());

        return CloneIssueResponse.builder()
                .originalIssueId(issueId)
                .originalIssueKey(original.getIssueKey())
                .clonedIssueId(clone.getId())
                .clonedIssueKey(clone.getIssueKey())
                .clonedAt(LocalDateTime.now())
                .clonedBy(userId)
                .includeComments(includeComments)
                .includeAttachments(includeAttachments)
                .build();
    }

    @Transactional
    public IssueResponse cloneIssueToProject(UUID issueId, UUID targetProjectId, UUID userId) {
        log.info("Cloning issue {} to project {} by user {}", issueId, targetProjectId, userId);

        Issue original = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        // Get new project key for new issue key
        String newIssueKey = "NEW-" + System.currentTimeMillis() % 10000;

        Issue clone = Issue.builder()
                .projectId(targetProjectId)
                .issueKey(newIssueKey)
                .title(original.getTitle())
                .description(original.getDescription())
                .status(original.getStatus())
                .priority(original.getPriority())
                .issueType(original.getIssueType())
                .reporterId(userId)
                .assigneeId(original.getAssigneeId())
                .resolutionId(original.getResolutionId())
                .labels(original.getLabels())
                .originalEstimate(original.getOriginalEstimate())
                .fixVersions(original.getFixVersions())
                .affectsVersions(original.getAffectsVersions())
                .storyPoints(original.getStoryPoints())
                .build();

        clone = issueRepository.save(clone);
        log.info("Issue cloned to project successfully: {} -> {}", original.getIssueKey(), clone.getIssueKey());

        return mapToResponse(clone);
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