package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.exception.InvalidTransitionException;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final IssuePriorityRepository issuePriorityRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final ProjectVersionRepository versionRepository;
    private final ProjectComponentRepository componentRepository;
    private final VoteRepository voteRepository;
    private final WatcherRepository watcherRepository;
    private final IssueLinkRepository issueLinkRepository;

    @Value("${workflow.service.url}")
    private String workflowServiceUrl;

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final UUID DEFAULT_STATUS_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_TYPE_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_PRIORITY_ID = UUID.fromString("b0000000-0000-0000-0000-000000000003");

    @Transactional
    public IssueResponse createIssue(CreateIssueRequest request, UUID currentUserId) {
        log.info("Creating issue in project: {} by user: {}", request.getProjectId(), currentUserId);

        // Get project key for issue key generation via REST
        String projectKey = getProjectKey(request.getProjectId());
        if (projectKey == null) {
            throw new ResourceNotFoundException("Project", "id", request.getProjectId());
        }

        IssueType issueType = issueTypeRepository.findById(
                request.getIssueTypeId() != null ? request.getIssueTypeId() : DEFAULT_TYPE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("IssueType", "id",
                        request.getIssueTypeId() != null ? request.getIssueTypeId() : DEFAULT_TYPE_ID));

        IssueStatus status = issueStatusRepository.findById(DEFAULT_STATUS_ID)
                .orElseThrow(() -> new ResourceNotFoundException("IssueStatus", "id", DEFAULT_STATUS_ID));

        IssuePriority priority = issuePriorityRepository.findById(
                request.getPriorityId() != null ? request.getPriorityId() : DEFAULT_PRIORITY_ID)
                .orElseThrow(() -> new ResourceNotFoundException("IssuePriority", "id",
                        request.getPriorityId() != null ? request.getPriorityId() : DEFAULT_PRIORITY_ID));

        String issueKey = generateIssueKey(projectKey);

        Issue issue = Issue.builder()
                .projectId(request.getProjectId())
                .issueKey(issueKey)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(status)
                .priority(priority)
                .issueType(issueType)
                .reporterId(currentUserId)
                .assigneeId(request.getAssigneeId())
                .parentIssueId(request.getParentIssueId())
                // Epic fields
                .epicId(request.getEpicId())
                .epicName(request.getEpicName())
                .epicColor(request.getEpicColor())
                // Security
                .securityLevelId(request.getSecurityLevelId())
                // Versions
                .affectsVersions(request.getAffectsVersions())
                .fixVersions(request.getFixVersions())
                // Story points and rank
                .storyPoints(request.getStoryPoints())
                .rank(request.getRank())
                // Time tracking
                .originalEstimate(request.getOriginalEstimate())
                .remainingEstimate(request.getRemainingEstimate())
                .timeSpent(request.getTimeSpent())
                // Resolution
                .resolutionId(request.getResolutionId())
                // Due date
                .dueDate(request.getDueDate())
                .build();

        issue = issueRepository.save(issue);
        log.info("Issue created successfully: {}", issueKey);
        return mapToIssueResponse(issue);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> searchIssues(IssueSearchRequest request) {
        log.debug("Searching issues with filters: {}", request);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Issue> issues;
        if (request.getProjectId() != null) {
            issues = issueRepository.findByProjectId(request.getProjectId(), pageable);
        } else {
            issues = issueRepository.findAll(pageable);
        }

        return issues.map(this::mapToIssueResponse);
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssue(UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        return mapToIssueResponse(issue);
    }

    @Transactional
    public IssueResponse updateIssue(UUID issueId, UpdateIssueRequest request) {
        log.info("Updating issue: {}", issueId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        // Optimistic locking: check version if provided
        if (request.getExpectedVersion() != null) {
            if (!request.getExpectedVersion().equals(issue.getVersion())) {
                throw new OptimisticLockException(
                        "Issue has been modified by another user. Expected version: " +
                        request.getExpectedVersion() + ", current version: " + issue.getVersion());
            }
        }

        if (request.getTitle() != null) {
            issue.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            issue.setDescription(request.getDescription());
        }
        if (request.getAssigneeId() != null) {
            issue.setAssigneeId(request.getAssigneeId());
        }
        if (request.getPriorityId() != null) {
            IssuePriority priority = issuePriorityRepository.findById(request.getPriorityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Priority", "id", request.getPriorityId()));
            issue.setPriority(priority);
        }
        if (request.getIssueTypeId() != null) {
            IssueType issueType = issueTypeRepository.findById(request.getIssueTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("IssueType", "id", request.getIssueTypeId()));
            issue.setIssueType(issueType);
        }

        // Epic fields
        if (request.getEpicId() != null) issue.setEpicId(request.getEpicId());
        if (request.getEpicName() != null) issue.setEpicName(request.getEpicName());
        if (request.getEpicColor() != null) issue.setEpicColor(request.getEpicColor());

        // Security
        if (request.getSecurityLevelId() != null) issue.setSecurityLevelId(request.getSecurityLevelId());

        // Parent issue
        if (request.getParentIssueId() != null) issue.setParentIssueId(request.getParentIssueId());

        // Versions
        if (request.getAffectsVersions() != null) issue.setAffectsVersions(request.getAffectsVersions());
        if (request.getFixVersions() != null) issue.setFixVersions(request.getFixVersions());

        // Story points and rank
        if (request.getStoryPoints() != null) issue.setStoryPoints(request.getStoryPoints());
        if (request.getRank() != null) issue.setRank(request.getRank());

        // Time tracking
        if (request.getOriginalEstimate() != null) issue.setOriginalEstimate(request.getOriginalEstimate());
        if (request.getRemainingEstimate() != null) issue.setRemainingEstimate(request.getRemainingEstimate());
        if (request.getTimeSpent() != null) issue.setTimeSpent(request.getTimeSpent());

        // Resolution
        if (request.getResolutionId() != null) issue.setResolutionId(request.getResolutionId());
        if (request.getResolutionDate() != null) issue.setResolutionDate(request.getResolutionDate());

        // Due date
        if (request.getDueDate() != null) issue.setDueDate(request.getDueDate());

        issue = issueRepository.save(issue);
        log.info("Issue updated successfully: {}", issueId);
        return mapToIssueResponse(issue);
    }

    @Transactional
    public IssueResponse updateIssueStatus(UUID issueId, UUID newStatusId, UUID projectId) {
        log.info("Updating status for issue {} to {}", issueId, newStatusId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        IssueStatus currentStatus = issue.getStatus();
        IssueStatus newStatus = issueStatusRepository.findById(newStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status", "id", newStatusId));

        if (!validateTransition(projectId, currentStatus.getId(), newStatusId)) {
            throw new InvalidTransitionException(
                    String.format("Invalid transition from '%s' to '%s'", currentStatus.getName(), newStatus.getName()));
        }

        issue.setStatus(newStatus);
        issue = issueRepository.save(issue);

        log.info("Issue status updated successfully: {} -> {}", issue.getIssueKey(), newStatus.getName());
        return mapToIssueResponse(issue);
    }

    @Transactional
    public void deleteIssue(UUID issueId) {
        log.info("Deleting issue: {}", issueId);

        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", "id", issueId);
        }

        issueRepository.deleteById(issueId);
        log.info("Issue deleted successfully: {}", issueId);
    }

    private boolean validateTransition(UUID projectId, UUID fromStatusId, UUID toStatusId) {
        try {
            String url = String.format("%s/api/workflows/project/%s/validate-transition?fromStatus=%s&toStatus=%s",
                    workflowServiceUrl, projectId, fromStatusId, toStatusId);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<ValidationResponse> response = restTemplate.getForEntity(url, ValidationResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().isValid();
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to validate transition with workflow service, allowing by default: {}", e.getMessage());
            return true;
        }
    }

    private String generateIssueKey(String projectKey) {
        String normalizedKey = projectKey.substring(0, Math.min(projectKey.length(), 6)).toUpperCase();

        Integer maxNumber = issueRepository.findMaxIssueNumberByProjectKey(normalizedKey)
                .orElse(0);

        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;

        return normalizedKey + "-" + nextNumber;
    }

    private IssueResponse mapToIssueResponse(Issue issue) {
        IssueResponse.IssueResponseBuilder builder = IssueResponse.builder()
                .id(issue.getId())
                .projectId(issue.getProjectId())
                .issueKey(issue.getIssueKey())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .reporterId(issue.getReporterId())
                .assigneeId(issue.getAssigneeId())
                .parentIssueId(issue.getParentIssueId())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                // Epic fields
                .epicId(issue.getEpicId())
                .epicName(issue.getEpicName())
                .epicColor(issue.getEpicColor())
                // Security
                .securityLevelId(issue.getSecurityLevelId())
                // Versions
                .affectsVersions(issue.getAffectsVersions())
                .fixVersions(issue.getFixVersions())
                // Story points and rank
                .storyPoints(issue.getStoryPoints())
                .rank(issue.getRank())
                // Time tracking
                .originalEstimate(issue.getOriginalEstimate())
                .remainingEstimate(issue.getRemainingEstimate())
                .timeSpent(issue.getTimeSpent())
                // Resolution
                .resolutionId(issue.getResolutionId())
                .resolutionDate(issue.getResolutionDate())
                // Due date
                .dueDate(issue.getDueDate())
                // Votes and watchers
                .voteCount(issue.getVoteCount())
                .watcherCount(issue.getWatcherCount());

        if (issue.getStatus() != null) {
            builder.statusId(issue.getStatus().getId())
                    .statusName(issue.getStatus().getName())
                    .statusCategory(issue.getStatus().getCategory());
        }
        if (issue.getPriority() != null) {
            builder.priorityId(issue.getPriority().getId())
                    .priorityName(issue.getPriority().getName())
                    .priorityColor(issue.getPriority().getColor());
        }
        if (issue.getIssueType() != null) {
            builder.issueTypeId(issue.getIssueType().getId())
                    .issueTypeName(issue.getIssueType().getName())
                    .issueTypeIcon(issue.getIssueType().getIcon());
        }

        // Calculate work ratio
        if (issue.getOriginalEstimate() != null && issue.getOriginalEstimate() > 0) {
            long spent = issue.getTimeSpent() != null ? issue.getTimeSpent() : 0;
            double ratio = (double) spent / issue.getOriginalEstimate() * 100;
            builder.workRatio(Math.round(ratio * 100.0) / 100.0);
        }

        // Load version names
        if (issue.getAffectsVersions() != null && issue.getAffectsVersions().length > 0) {
            List<String> versionNames = new ArrayList<>();
            for (UUID versionId : issue.getAffectsVersions()) {
                versionRepository.findById(versionId).ifPresent(v -> versionNames.add(v.getName()));
            }
            builder.affectsVersionNames(versionNames.isEmpty() ? null : versionNames.toArray(new String[0]));
        }
        if (issue.getFixVersions() != null && issue.getFixVersions().length > 0) {
            List<String> versionNames = new ArrayList<>();
            for (UUID versionId : issue.getFixVersions()) {
                versionRepository.findById(versionId).ifPresent(v -> versionNames.add(v.getName()));
            }
            builder.fixVersionNames(versionNames.isEmpty() ? null : versionNames.toArray(new String[0]));
        }

        // Load component names
        if (issue.getComponentIds() != null && issue.getComponentIds().length > 0) {
            List<String> componentNames = new ArrayList<>();
            for (UUID componentId : issue.getComponentIds()) {
                componentRepository.findById(componentId).ifPresent(c -> componentNames.add(c.getName()));
            }
            builder.componentNames(componentNames.isEmpty() ? null : componentNames.toArray(new String[0]));
        }

        // Load linked issues
        List<IssueLink> links = issueLinkRepository.findBySourceIssueId(issue.getId());
        if (!links.isEmpty()) {
            // Batch load all destination issues to avoid N+1
            Set<UUID> destinationIds = links.stream()
                    .map(IssueLink::getDestinationIssueId)
                    .collect(Collectors.toSet());
            Map<UUID, Issue> issuesById = issueRepository.findAllById(destinationIds).stream()
                    .collect(Collectors.toMap(Issue::getId, i -> i));

            List<IssueResponse.LinkedIssueInfo> linkedIssues = links.stream()
                    .map(link -> {
                        Issue linkedIssue = issuesById.get(link.getDestinationIssueId());
                        if (linkedIssue == null) return null;
                        return IssueResponse.LinkedIssueInfo.builder()
                                .linkType(link.getLinkType() != null ? link.getLinkType() : "Related")
                                .issueId(linkedIssue.getId())
                                .issueKey(linkedIssue.getIssueKey())
                                .title(linkedIssue.getTitle())
                                .direction("OUTWARD")
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            builder.linkedIssues(linkedIssues);
        }

        // Load subtasks
        List<Issue> subtasks = issueRepository.findByParentIssueId(issue.getId());
        if (!subtasks.isEmpty()) {
            builder.subTaskCount(subtasks.size())
                    .subtasks(subtasks.stream()
                            .map(this::mapToIssueResponse)
                            .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * Search issues using JQL query (basic implementation)
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> searchByJql(String jql, int page, int pageSize) {
        log.info("Searching issues with JQL: {}", jql);

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Issue> issues = issueRepository.findAll(pageable);

        java.util.List<java.util.Map<String, Object>> issuesList = issues.getContent().stream()
                .map(this::mapIssueToMap)
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("issues", issuesList);
        result.put("totalCount", issues.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * Get multiple issues by their IDs
     */
    @Transactional(readOnly = true)
    public java.util.List<IssueResponse> getIssuesByIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return java.util.List.of();
        }

        java.util.List<UUID> uuidList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(java.util.stream.Collectors.toList());

        return issueRepository.findAllById(uuidList).stream()
                .map(this::mapToIssueResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private java.util.Map<String, Object> mapIssueToMap(Issue issue) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", issue.getId());
        map.put("key", issue.getIssueKey());
        map.put("self", "/api/issues/" + issue.getId());

        java.util.Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("summary", issue.getTitle());
        fields.put("description", issue.getDescription());

        if (issue.getStatus() != null) {
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("id", issue.getStatus().getId());
            status.put("name", issue.getStatus().getName());
            status.put("statusCategory", java.util.Map.of("name", issue.getStatus().getCategory()));
            fields.put("status", status);
        }

        if (issue.getPriority() != null) {
            java.util.Map<String, Object> priority = new java.util.HashMap<>();
            priority.put("id", issue.getPriority().getId());
            priority.put("name", issue.getPriority().getName());
            fields.put("priority", priority);
        }

        if (issue.getIssueType() != null) {
            java.util.Map<String, Object> issueType = new java.util.HashMap<>();
            issueType.put("id", issue.getIssueType().getId());
            issueType.put("name", issue.getIssueType().getName());
            issueType.put("iconUrl", issue.getIssueType().getIcon());
            fields.put("issuetype", issueType);
        }

        fields.put("assignee", issue.getAssigneeId() != null ?
                java.util.Map.of("id", issue.getAssigneeId()) : null);
        fields.put("reporter", issue.getReporterId() != null ?
                java.util.Map.of("id", issue.getReporterId()) : null);
        fields.put("created", issue.getCreatedAt());
        fields.put("updated", issue.getUpdatedAt());
        fields.put("storyPoints", issue.getStoryPoints());

        map.put("fields", fields);
        return map;
    }

    private String getProjectKey(UUID projectId) {
        try {
            String url = String.format("%s/api/projects/%s", projectServiceUrl, projectId);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && response.get("projectKey") != null) {
                return response.get("projectKey").toString();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get project key for {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    @lombok.Data
    private static class ValidationResponse {
        private boolean valid;
    }
}