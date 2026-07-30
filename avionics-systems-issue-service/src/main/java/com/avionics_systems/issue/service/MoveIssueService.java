package com.avionics_systems.issue.service;

import com.avionics_systems.issue.client.ProjectServiceClient;
import com.avionics_systems.issue.dto.ChangeItemResponse;
import com.avionics_systems.issue.dto.IssueResponse;
import com.avionics_systems.issue.entity.Issue;
import com.avionics_systems.issue.event.IssueEventOutboxPublisher;
import com.avionics_systems.issue.exception.ResourceNotFoundException;
import com.avionics_systems.issue.repository.IssueRepository;
import com.avionics_systems.issue.repository.IssueStatusRepository;
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
    private final ProjectServiceClient projectServiceClient;

    @Value("${workflow.service.url}")
    private String workflowServiceUrl;

    private final RestTemplate restTemplate;

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

        validateIssueTypeInTargetProject(issue, targetProjectId);

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
            String movePayload = String.format(
                    "{\"oldProjectId\":\"%s\",\"newProjectId\":\"%s\",\"oldKey\":\"%s\",\"newKey\":\"%s\",\"oldProjectKey\":\"%s\",\"newProjectKey\":\"%s\"}",
                    sourceProjectId, targetProjectId, oldIssueKey, newIssueKey, sourceProjectKey, targetProjectKey);
            eventOutboxPublisher.publish("ISSUE_MOVED", issue.getId(), targetProjectId, movePayload);
        } catch (Exception e) {
            log.error("Failed to publish ISSUE_MOVED event for {}: {}", issue.getId(), e.getMessage(), e);
        }

        return issueService.getIssue(issue.getId());
    }

    private String getProjectKey(UUID projectId) {
        return projectServiceClient.getProjectKey(projectId);
    }

    private String generateIssueKey(String projectKey) {
        String normalizedKey = projectKey.toUpperCase();
        Integer maxNumber = issueRepository.findMaxIssueNumberByProjectKeyForUpdate(normalizedKey).orElse(0);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return normalizedKey + "-" + nextNumber;
    }

    private void validateIssueTypeInTargetProject(Issue issue, UUID targetProjectId) {
        if (issue.getIssueType() == null) {
            return;
        }
        if (!projectServiceClient.isIssueTypeValidInProject(targetProjectId, issue.getIssueType().getId())) {
            throw new IllegalArgumentException("Issue type '" + issue.getIssueType().getName()
                    + "' is not available in the target project's issue type scheme");
        }
    }

    @SuppressWarnings("unchecked")
    private UUID resolveTargetStatus(Issue issue, UUID targetProjectId) {
        if (issue.getStatus() == null) {
            return null;
        }
        try {
            String url = String.format(
                    "%s/api/workflows/project/%s/statuses",
                    workflowServiceUrl, targetProjectId);
            List<Map<String, Object>> statuses = restTemplate.getForObject(url, List.class);
            if (statuses != null) {
                boolean currentStatusValid = statuses.stream()
                        .anyMatch(s -> issue.getStatus().getId().toString().equals(String.valueOf(s.get("id"))));
                if (currentStatusValid) {
                    return issue.getStatus().getId();
                }
                return statuses.stream()
                        .filter(s -> "TODO".equals(s.get("category")))
                        .map(s -> UUID.fromString(String.valueOf(s.get("id"))))
                        .findFirst()
                        .orElse(issue.getStatus().getId());
            }
        } catch (Exception e) {
            log.info("Could not resolve target project statuses, keeping current: {}", e.getMessage());
        }
        return issue.getStatus().getId();
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
                    .fieldType("avionics-systems")
                    .field("Project")
                    .oldValue(sourceProjectId.toString())
                    .oldString(sourceProjectKey)
                    .newValue(targetProjectId.toString())
                    .newString(targetProjectKey)
                    .build());
            changes.add(ChangeItemResponse.builder()
                    .fieldType("avionics-systems")
                    .field("Key")
                    .oldValue(oldKey)
                    .oldString(oldKey)
                    .newValue(newKey)
                    .newString(newKey)
                    .build());
            changeHistoryService.recordChange(issueId, userId, null, changes);
        } catch (Exception e) {
            log.error("Failed to record move history for issue {}: {}", issueId, e.getMessage(), e);
        }
    }
}