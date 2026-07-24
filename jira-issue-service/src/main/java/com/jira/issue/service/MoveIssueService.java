package com.jira.issue.service;

import com.jira.issue.dto.ChangeItemResponse;
import com.jira.issue.dto.IssueResponse;
import com.jira.issue.entity.Issue;
import com.jira.issue.event.IssueEventOutboxPublisher;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueRepository;
import com.jira.issue.repository.IssueStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoveIssueService {

    private final IssueRepository issueRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final ChangeHistoryService changeHistoryService;
    private final IssueEventOutboxPublisher eventOutboxPublisher;
    private final IssueService issueService;

    @Value("${project.service.url}")
    private String projectServiceUrl;

    @Value("${workflow.service.url}")
    private String workflowServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public IssueResponse moveIssue(UUID issueId, UUID targetProjectId, UUID userId) {
        log.info("Moving issue {} to project {} by user {}", issueId, targetProjectId, userId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        UUID sourceProjectId = issue.getProjectId();
        String oldIssueKey = issue.getIssueKey();
        String sourceProjectKey = extractProjectKeyFromIssueKey(oldIssueKey);

        String targetProjectKey = getProjectKey(targetProjectId);
        if (targetProjectKey == null) {
            throw new ResourceNotFoundException("Project", "id", targetProjectId);
        }

        if (sourceProjectId.equals(targetProjectId)) {
            log.info("Issue {} is already in target project, no move needed", issueId);
            return issueService.getIssue(issueId);
        }

        String newIssueKey = generateIssueKey(targetProjectKey);

        UUID resolvedStatusId = resolveTargetStatus(issue, targetProjectId);

        issue.setProjectId(targetProjectId);
        issue.setIssueKey(newIssueKey);
        if (resolvedStatusId != null && !resolvedStatusId.equals(issue.getStatus().getId())) {
            issueStatusRepository.findById(resolvedStatusId).ifPresent(issue::setStatus);
        }

        issue = issueRepository.save(issue);
        log.info("Issue moved: {} -> {} (project {})", oldIssueKey, newIssueKey, targetProjectKey);

        recordMoveHistory(issue.getId(), userId, sourceProjectId, targetProjectId,
                sourceProjectKey, targetProjectKey, oldIssueKey, newIssueKey);

        try {
            eventOutboxPublisher.publish("ISSUE_MOVED", issue.getId(), targetProjectId);
        } catch (Exception e) {
            log.warn("Failed to publish ISSUE_MOVED event for {}: {}", issue.getId(), e.getMessage());
        }

        return issueService.getIssue(issue.getId());
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
        String normalizedKey = projectKey.substring(0, Math.min(projectKey.length(), 6)).toUpperCase();
        synchronized (this) {
            Integer maxNumber = issueRepository.findMaxIssueNumberByProjectKey(normalizedKey).orElse(0);
            int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
            return normalizedKey + "-" + nextNumber;
        }
    }

    private UUID resolveTargetStatus(Issue issue, UUID targetProjectId) {
        if (issue.getStatus() == null) {
            return null;
        }
        try {
            String url = String.format(
                    "%s/api/workflows/project/%s/validate-transition?fromStatus=%s&toStatus=%s",
                    workflowServiceUrl, targetProjectId,
                    issue.getStatus().getId(), issue.getStatus().getId());
            restTemplate.getForObject(url, Map.class);
            return issue.getStatus().getId();
        } catch (Exception e) {
            log.info("Current status {} not valid in target project, falling back to default", issue.getStatus().getName());
            return issueStatusRepository.findAll().stream()
                    .filter(s -> "TODO".equals(s.getCategory()))
                    .min((a, b) -> Integer.compare(a.getSequence(), b.getSequence()))
                    .map(s -> s.getId())
                    .orElse(issue.getStatus().getId());
        }
    }

    private String extractProjectKeyFromIssueKey(String issueKey) {
        if (issueKey == null || !issueKey.contains("-")) {
            return "UNKNOWN";
        }
        return issueKey.substring(0, issueKey.lastIndexOf('-'));
    }

    private void recordMoveHistory(UUID issueId, UUID userId, UUID sourceProjectId,
                                   UUID targetProjectId, String sourceProjectKey,
                                   String targetProjectKey, String oldKey, String newKey) {
        try {
            List<ChangeItemResponse> changes = new ArrayList<>();
            changes.add(ChangeItemResponse.builder()
                    .fieldType("jira")
                    .field("Project")
                    .oldValue(sourceProjectId.toString())
                    .oldString(sourceProjectKey)
                    .newValue(targetProjectId.toString())
                    .newString(targetProjectKey)
                    .build());
            changes.add(ChangeItemResponse.builder()
                    .fieldType("jira")
                    .field("Key")
                    .oldValue(oldKey)
                    .oldString(oldKey)
                    .newValue(newKey)
                    .newString(newKey)
                    .build());
            changeHistoryService.recordChange(issueId, userId, null, changes);
        } catch (Exception e) {
            log.warn("Failed to record move history for issue {}: {}", issueId, e.getMessage());
        }
    }
}