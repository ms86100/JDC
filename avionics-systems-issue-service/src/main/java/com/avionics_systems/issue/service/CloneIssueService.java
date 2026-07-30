package com.avionics_systems.issue.service;

import com.avionics_systems.issue.client.ProjectServiceClient;
import com.avionics_systems.issue.dto.CloneIssueResponse;
import com.avionics_systems.issue.dto.IssueResponse;
import com.avionics_systems.issue.entity.Issue;
import com.avionics_systems.issue.entity.IssueStatus;
import com.avionics_systems.issue.event.IssueEventOutboxPublisher;
import com.avionics_systems.issue.exception.ResourceNotFoundException;
import com.avionics_systems.issue.repository.IssueRepository;
import com.avionics_systems.issue.repository.IssueStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class CloneIssueService {

    private final IssueRepository issueRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final IssueEventOutboxPublisher eventOutboxPublisher;
    private final IssueService issueService;
    private final ProjectServiceClient projectServiceClient;

    @Value("${app.defaults.status-id:00000000-0000-0000-0001-000000000002}")
    private String defaultStatusIdStr;

    @Value("${app.defaults.clone-title-suffix: (Copy)}")
    private String cloneTitleSuffix;

    public CloneIssueService(IssueRepository issueRepository,
                             IssueStatusRepository issueStatusRepository,
                             IssueEventOutboxPublisher eventOutboxPublisher,
                             IssueService issueService,
                             ProjectServiceClient projectServiceClient) {
        this.issueRepository = issueRepository;
        this.issueStatusRepository = issueStatusRepository;
        this.eventOutboxPublisher = eventOutboxPublisher;
        this.issueService = issueService;
        this.projectServiceClient = projectServiceClient;
    }

    private UUID getDefaultStatusId() { return UUID.fromString(defaultStatusIdStr); }

    @Transactional
    public CloneIssueResponse cloneIssue(UUID issueId, UUID userId, boolean includeComments, boolean includeAttachments) {
        log.info("Cloning issue {} by user {}", issueId, userId);

        Issue original = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        String projectKey = extractProjectKeyFromIssueKey(original.getIssueKey());
        String newIssueKey = generateIssueKey(projectKey);

        IssueStatus initialStatus = issueStatusRepository.findById(getDefaultStatusId())
                .orElseThrow(() -> new ResourceNotFoundException("IssueStatus", "id", getDefaultStatusId()));

        Issue clone = Issue.builder()
                .projectId(original.getProjectId())
                .issueKey(newIssueKey)
                .title(original.getTitle() + cloneTitleSuffix)
                .description(original.getDescription())
                .status(initialStatus)
                .priority(original.getPriority())
                .issueType(original.getIssueType())
                .reporterId(userId)
                .assigneeId(original.getAssigneeId())
                .resolutionId(null)
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

        IssueStatus initialStatus = issueStatusRepository.findById(getDefaultStatusId())
                .orElseThrow(() -> new ResourceNotFoundException("IssueStatus", "id", getDefaultStatusId()));

        Issue clone = Issue.builder()
                .projectId(targetProjectId)
                .issueKey(newIssueKey)
                .title(original.getTitle())
                .description(original.getDescription())
                .status(initialStatus)
                .priority(original.getPriority())
                .issueType(original.getIssueType())
                .reporterId(userId)
                .assigneeId(original.getAssigneeId())
                .resolutionId(null)
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
        return projectServiceClient.getProjectKey(projectId);
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