package com.jira.issue.service;

import com.jira.issue.dto.CloneIssueResponse;
import com.jira.issue.dto.IssueResponse;
import com.jira.issue.entity.Issue;
import com.jira.issue.event.IssueEventOutboxPublisher;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloneIssueService {

    private final IssueRepository issueRepository;
    private final IssueEventOutboxPublisher eventOutboxPublisher;
    private final IssueService issueService;

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate;

    @Transactional
    public CloneIssueResponse cloneIssue(UUID issueId, UUID userId, boolean includeComments, boolean includeAttachments) {
        log.info("Cloning issue {} by user {}", issueId, userId);

        Issue original = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        String projectKey = extractProjectKeyFromIssueKey(original.getIssueKey());
        String newIssueKey = generateIssueKey(projectKey);

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

        try {
            eventOutboxPublisher.publish("ISSUE_CREATED", clone.getId(), clone.getProjectId());
        } catch (Exception e) {
            log.warn("Failed to publish clone event for {}: {}", clone.getId(), e.getMessage());
        }

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

        String targetProjectKey = getProjectKey(targetProjectId);
        if (targetProjectKey == null) {
            throw new ResourceNotFoundException("Project", "id", targetProjectId);
        }
        String newIssueKey = generateIssueKey(targetProjectKey);

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

        try {
            eventOutboxPublisher.publish("ISSUE_CREATED", clone.getId(), targetProjectId);
        } catch (Exception e) {
            log.warn("Failed to publish clone event for {}: {}", clone.getId(), e.getMessage());
        }

        return issueService.getIssue(clone.getId());
    }

    private String getProjectKey(UUID projectId) {
        try {
            String url = String.format("%s/api/projects/%s", projectServiceUrl, projectId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("projectKey") != null) {
                return response.get("projectKey").toString();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get project key for {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    private String generateIssueKey(String projectKey) {
        String normalizedKey = projectKey.toUpperCase();
        Integer maxNumber = issueRepository.findMaxIssueNumberByProjectKeyForUpdate(normalizedKey).orElse(0);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return normalizedKey + "-" + nextNumber;
    }

    private String extractProjectKeyFromIssueKey(String issueKey) {
        if (issueKey == null || !issueKey.contains("-")) {
            return "UNKNOWN";
        }
        return issueKey.substring(0, issueKey.lastIndexOf('-'));
    }
}