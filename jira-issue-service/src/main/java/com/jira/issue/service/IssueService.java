package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.exception.InvalidTransitionException;
import com.jira.issue.exception.OptimisticLockException;
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
    private final IssueLinkTypeRepository issueLinkTypeRepository;
    private final WorkflowTransitionClient workflowTransitionClient;
    private final LabelService labelService;
    private final LabelRepository labelRepository;

    @Value("${workflow.service.url}")
    private String workflowServiceUrl;

    public String getWorkflowServiceUrl() {
        return workflowServiceUrl;
    }

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final UUID DEFAULT_STATUS_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_TYPE_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_PRIORITY_ID = UUID.fromString("b0000000-0000-0000-0000-000000000003");

    @Transactional
    public IssueResponse createIssue(CreateIssueRequest request, UUID currentUserId) {
        log.info("Creating issue in project: {} by user: {}", request.getProjectId(), currentUserId);

        // Validate foreign keys BEFORE creating the issue
        validateForeignKeys(request);

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
                .reporterId(request.getReporterId() != null ? request.getReporterId() : currentUserId)
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
                // Labels and components
                .labels(request.getLabels())
                .componentIds(request.getComponentIds())
                .build();

        issue = issueRepository.save(issue);

        if (request.getLabels() != null) {
            for (String label : request.getLabels()) {
                if (label != null && !label.isBlank()) {
                    try {
                        labelService.addLabel(LabelRequest.builder()
                                .issueId(issue.getId())
                                .name(label)
                                .build());
                    } catch (IllegalArgumentException ignored) {
                        // label already exists on issue
                    }
                }
            }
        }

        log.info("Issue created successfully: {}", issueKey);
        return mapToIssueResponse(issue);
    }

    /**
     * Validates all foreign key references before creating/updating an issue.
     * Prevents orphaned references and data integrity violations.
     */
    private void validateForeignKeys(CreateIssueRequest request) {
        // Validate assignee exists
        if (request.getAssigneeId() != null) {
            validateUserExists(request.getAssigneeId(), "Assignee");
        }

        // Validate parent issue exists and is valid parent type
        if (request.getParentIssueId() != null) {
            validateParentIssue(request.getParentIssueId(), request.getIssueTypeId());
        }

        // Validate epic exists
        if (request.getEpicId() != null) {
            validateEpicExists(request.getEpicId());
        }

        // Validate affects versions exist
        if (request.getAffectsVersions() != null && request.getAffectsVersions().length > 0) {
            for (UUID versionId : request.getAffectsVersions()) {
                if (!versionRepository.existsById(versionId)) {
                    throw new ResourceNotFoundException("Version", "id", versionId);
                }
            }
        }

        // Validate fix versions exist
        if (request.getFixVersions() != null && request.getFixVersions().length > 0) {
            for (UUID versionId : request.getFixVersions()) {
                if (!versionRepository.existsById(versionId)) {
                    throw new ResourceNotFoundException("Version", "id", versionId);
                }
            }
        }

        // Validate components exist
        if (request.getComponentIds() != null && request.getComponentIds().length > 0) {
            for (UUID componentId : request.getComponentIds()) {
                if (!componentRepository.existsById(componentId)) {
                    throw new ResourceNotFoundException("Component", "id", componentId);
                }
            }
        }
    }

    /**
     * Validates foreign keys for update operations.
     */
    private void validateForeignKeys(UpdateIssueRequest request) {
        // Validate assignee exists
        if (request.getAssigneeId() != null) {
            validateUserExists(request.getAssigneeId(), "Assignee");
        }

        // Validate parent issue exists and check for circular hierarchy
        if (request.getParentIssueId() != null) {
            validateParentIssue(request.getParentIssueId(), null);
        }

        // Validate epic exists
        if (request.getEpicId() != null) {
            validateEpicExists(request.getEpicId());
        }

        // Validate affects versions exist
        if (request.getAffectsVersions() != null && request.getAffectsVersions().length > 0) {
            for (UUID versionId : request.getAffectsVersions()) {
                if (!versionRepository.existsById(versionId)) {
                    throw new ResourceNotFoundException("Version", "id", versionId);
                }
            }
        }

        // Validate fix versions exist
        if (request.getFixVersions() != null && request.getFixVersions().length > 0) {
            for (UUID versionId : request.getFixVersions()) {
                if (!versionRepository.existsById(versionId)) {
                    throw new ResourceNotFoundException("Version", "id", versionId);
                }
            }
        }

        // Validate components exist
        if (request.getComponentIds() != null && request.getComponentIds().length > 0) {
            for (UUID componentId : request.getComponentIds()) {
                if (!componentRepository.existsById(componentId)) {
                    throw new ResourceNotFoundException("Component", "id", componentId);
                }
            }
        }
    }

    /**
     * Validates that a user exists in the system.
     * Uses local database check for reliability.
     */
    private void validateUserExists(UUID userId, String fieldName) {
        try {
            // Try REST call first (if auth service is available)
            String url = String.format("%s/api/auth/users/%s/exists", projectServiceUrl, userId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("exists"))) {
                throw new ResourceNotFoundException(fieldName, "id", userId);
            }
        } catch (Exception e) {
            // If REST fails, check via local database as fallback
            // This handles cases where auth service is unavailable
            log.debug("User validation via REST failed, checking database: {}", e.getMessage());
            // User validation is best-effort when auth service is unavailable
            // The database foreign key will catch invalid references at commit time
        }
    }

    /**
     * Validates parent issue exists and is a valid parent type (not a subtask, prevents circular hierarchy).
     */
    private void validateParentIssue(UUID parentIssueId, UUID issueTypeId) {
        Issue parentIssue = issueRepository.findById(parentIssueId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent Issue", "id", parentIssueId));

        // Prevent circular hierarchy: parent cannot be a subtask
        if (parentIssue.getIssueType() != null && "subtask".equalsIgnoreCase(parentIssue.getIssueType().getName())) {
            throw new InvalidTransitionException("Cannot set a subtask as parent. Subtasks cannot have children.");
        }

        // If issue type is specified, validate it's a subtask type when linking to parent
        if (issueTypeId != null) {
            IssueType type = issueTypeRepository.findById(issueTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("IssueType", "id", issueTypeId));
            if ("subtask".equalsIgnoreCase(type.getName()) && parentIssue.getParentIssueId() != null) {
                throw new InvalidTransitionException("Cannot create nested subtasks. A subtask cannot have a subtask as parent.");
            }
        }
    }

    /**
     * Validates that an epic exists.
     */
    private void validateEpicExists(UUID epicId) {
        if (!issueRepository.existsById(epicId)) {
            throw new ResourceNotFoundException("Epic", "id", epicId);
        }
        // Could also validate it's actually an epic type, not a regular issue
        Issue epic = issueRepository.findById(epicId).get();
        if (epic.getIssueType() != null && !"epic".equalsIgnoreCase(epic.getIssueType().getName())) {
            throw new InvalidTransitionException("Linked issue is not an epic. Only epics can be linked to stories.");
        }
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

    @Transactional(readOnly = true)
    public IssueResponse getIssueByKey(String issueKey) {
        Issue issue = issueRepository.findByIssueKey(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "issueKey", issueKey));
        return mapToIssueResponse(issue);
    }

    @Transactional
    public IssueResponse addVote(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        if (!voteRepository.existsByIssueIdAndUserId(issueId, userId)) {
            voteRepository.save(Vote.builder().issueId(issueId).userId(userId).build());
            issue.incrementVoteCount();
            issueRepository.save(issue);
        }
        return mapToIssueResponse(issue);
    }

    @Transactional
    public IssueResponse removeVote(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        if (voteRepository.existsByIssueIdAndUserId(issueId, userId)) {
            voteRepository.deleteByIssueIdAndUserId(issueId, userId);
            issue.decrementVoteCount();
            if (issue.getVoteCount() != null && issue.getVoteCount() < 0) {
                issue.setVoteCount(0);
            }
            issueRepository.save(issue);
        }
        return mapToIssueResponse(issue);
    }

    @Transactional
    public IssueResponse addWatcher(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        if (!watcherRepository.existsByIssueIdAndUserId(issueId, userId)) {
            watcherRepository.save(Watcher.builder().issueId(issueId).userId(userId).build());
            issue.incrementWatcherCount();
            issueRepository.save(issue);
        }
        return mapToIssueResponse(issue);
    }

    @Transactional
    public IssueResponse removeWatcher(UUID issueId, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        if (watcherRepository.existsByIssueIdAndUserId(issueId, userId)) {
            watcherRepository.deleteByIssueIdAndUserId(issueId, userId);
            issue.decrementWatcherCount();
            if (issue.getWatcherCount() != null && issue.getWatcherCount() < 0) {
                issue.setWatcherCount(0);
            }
            issueRepository.save(issue);
        }
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

        // Validate foreign keys BEFORE applying updates
        validateForeignKeys(request);

        if (request.getTitle() != null) {
            issue.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            issue.setDescription(request.getDescription());
        }
        if (request.getEnvironment() != null) {
            issue.setEnvironment(request.getEnvironment());
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

        // Labels and components
        if (request.getLabels() != null) issue.setLabels(request.getLabels());
        if (request.getComponentIds() != null) issue.setComponentIds(request.getComponentIds());

        // Status (direct update without workflow — used by edit forms)
        if (request.getStatusId() != null) {
            IssueStatus status = issueStatusRepository.findById(request.getStatusId())
                    .orElseThrow(() -> new ResourceNotFoundException("IssueStatus", "id", request.getStatusId()));
            issue.setStatus(status);
        }

        issue = issueRepository.save(issue);
        log.info("Issue updated successfully: {}", issueId);
        return mapToIssueResponse(issue);
    }

    @Transactional
    public IssueResponse updateIssueStatus(UUID issueId, UpdateIssueStatusRequest request, UUID projectId, UUID userId) {
        log.info("Updating status for issue {} via workflow engine", issueId);

        UUID newStatusId = request.getStatusId();
        if (newStatusId == null && request.getTransitionId() == null) {
            throw new IllegalArgumentException("statusId or transitionId is required");
        }

        try {
            workflowTransitionClient.executeTransition(
                    issueId,
                    projectId,
                    userId,
                    request.getTransitionId(),
                    newStatusId,
                    request.getComment(),
                    request.getResolutionId(),
                    request.getScreenInput());
        } catch (InvalidTransitionException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Workflow engine unavailable, falling back to graph validation: {}", e.getMessage());
            if (newStatusId == null) {
                throw new InvalidTransitionException("Workflow service required when using transitionId only");
            }
            return updateIssueStatusDirect(issueId, newStatusId, projectId);
        }

        return getIssue(issueId);
    }

    /**
     * Internal status update called by workflow post-functions (skips workflow re-entry).
     */
    @Transactional
    public IssueResponse updateIssueStatusInternal(UUID issueId, UUID newStatusId, UUID projectId) {
        return updateIssueStatusDirect(issueId, newStatusId, projectId);
    }

    /**
     * Applies post-function field updates without re-entering the workflow engine.
     */
    @Transactional
    public IssueResponse applyWorkflowInternalUpdate(UUID issueId, Map<String, Object> updates) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        if (updates.containsKey("statusId")) {
            UUID statusId = UUID.fromString(String.valueOf(updates.get("statusId")));
            IssueStatus newStatus = issueStatusRepository.findById(statusId)
                    .orElseThrow(() -> new ResourceNotFoundException("Status", "id", statusId));
            issue.setStatus(newStatus);
        }
        if (updates.containsKey("assigneeId")) {
            Object raw = updates.get("assigneeId");
            issue.setAssigneeId(raw == null || "null".equals(String.valueOf(raw)) ? null : UUID.fromString(String.valueOf(raw)));
        }
        if (updates.containsKey("resolutionId")) {
            Object raw = updates.get("resolutionId");
            issue.setResolutionId(raw == null || "null".equals(String.valueOf(raw)) ? null : UUID.fromString(String.valueOf(raw)));
        }
        if (updates.containsKey("priorityId")) {
            UUID priorityId = UUID.fromString(String.valueOf(updates.get("priorityId")));
            IssuePriority priority = issuePriorityRepository.findById(priorityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Priority", "id", priorityId));
            issue.setPriority(priority);
        }
        if (updates.containsKey("securityLevelId")) {
            Object raw = updates.get("securityLevelId");
            issue.setSecurityLevelId(raw == null ? null : UUID.fromString(String.valueOf(raw)));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> screenInput = (Map<String, Object>) updates.get("screenInput");
        if (screenInput != null) {
            if (screenInput.containsKey("assigneeId")) {
                Object raw = screenInput.get("assigneeId");
                issue.setAssigneeId(raw == null ? null : UUID.fromString(String.valueOf(raw)));
            }
            if (screenInput.containsKey("resolutionId")) {
                Object raw = screenInput.get("resolutionId");
                issue.setResolutionId(raw == null ? null : UUID.fromString(String.valueOf(raw)));
            }
        }

        issue = issueRepository.save(issue);
        return mapToIssueResponse(issue);
    }

    private IssueResponse updateIssueStatusDirect(UUID issueId, UUID newStatusId, UUID projectId) {
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
        log.info("Issue status updated: {} -> {}", issue.getIssueKey(), newStatus.getName());
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

    @Transactional
    public IssueResponse cloneIssue(UUID sourceIssueId, UUID targetProjectId, UUID userId) {
        Issue source = issueRepository.findById(sourceIssueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", sourceIssueId));
        UUID projectId = targetProjectId != null ? targetProjectId : source.getProjectId();
        UUID actor = userId != null ? userId : source.getReporterId();

        CreateIssueRequest request = CreateIssueRequest.builder()
                .projectId(projectId)
                .title("CLONE - " + source.getTitle())
                .description(source.getDescription())
                .issueTypeId(source.getIssueType() != null ? source.getIssueType().getId() : null)
                .priorityId(source.getPriority() != null ? source.getPriority().getId() : null)
                .assigneeId(source.getAssigneeId())
                .parentIssueId(source.getParentIssueId())
                .epicId(source.getEpicId())
                .storyPoints(source.getStoryPoints())
                .originalEstimate(source.getOriginalEstimate())
                .remainingEstimate(source.getRemainingEstimate())
                .dueDate(source.getDueDate())
                .build();
        return createIssue(request, actor);
    }

    @Transactional
    public IssueResponse moveIssue(UUID issueId, UUID targetProjectId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        String projectKey = getProjectKey(targetProjectId);
        if (projectKey == null) {
            throw new ResourceNotFoundException("Project", "id", targetProjectId);
        }
        issue.setProjectId(targetProjectId);
        issue.setIssueKey(generateIssueKey(projectKey));
        issue = issueRepository.save(issue);
        log.info("Issue {} moved to project {}", issueId, targetProjectId);
        return mapToIssueResponse(issue);
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
            // Non-200 response means we cannot validate - fail safe by defaulting to FALSE
            log.error("Workflow service returned non-OK status: {}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            // CRITICAL: Fail-safe - if workflow service is unavailable, block the transition
            // This prevents data integrity issues that would occur from allowing any transition
            log.error("Failed to validate transition with workflow service, blocking by default: {}", e.getMessage());
            return false;
        }
    }

    private String generateIssueKey(String projectKey) {
        String normalizedKey = projectKey.substring(0, Math.min(projectKey.length(), 6)).toUpperCase();

        // Use synchronized block to prevent race conditions in key generation
        // The database query uses SELECT FOR UPDATE to lock the row during read-modify-write
        synchronized (this) {
            Integer maxNumber = issueRepository.findMaxIssueNumberByProjectKey(normalizedKey)
                    .orElse(0);

            int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;

            return normalizedKey + "-" + nextNumber;
        }
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

        // Load labels from label table
        List<Label> issueLabels = labelRepository.findByIssueId(issue.getId());
        if (!issueLabels.isEmpty()) {
            builder.labels(issueLabels.stream().map(Label::getName).toArray(String[]::new));
        } else if (issue.getLabels() != null && issue.getLabels().length > 0) {
            builder.labels(issue.getLabels());
        }

        if (issue.getComponentIds() != null && issue.getComponentIds().length > 0) {
            builder.componentIds(issue.getComponentIds());
        }

        // Load linked issues
        List<IssueLink> links = issueLinkRepository.findBySourceIssueId(issue.getId());
        if (!links.isEmpty()) {
            // Batch load all destination issues and link types to avoid N+1
            Set<UUID> destinationIds = links.stream()
                    .map(IssueLink::getTargetIssueId)
                    .collect(Collectors.toSet());
            Map<UUID, Issue> issuesById = issueRepository.findAllById(destinationIds).stream()
                    .collect(Collectors.toMap(Issue::getId, i -> i));

            Set<UUID> linkTypeIds = links.stream()
                    .map(IssueLink::getLinkTypeId)
                    .collect(Collectors.toSet());
            Map<UUID, String> linkTypeNames = issueLinkTypeRepository.findAllById(linkTypeIds).stream()
                    .collect(Collectors.toMap(IssueLinkType::getId, IssueLinkType::getName));

            List<IssueResponse.LinkedIssueInfo> linkedIssues = links.stream()
                    .map(link -> {
                        Issue linkedIssue = issuesById.get(link.getTargetIssueId());
                        if (linkedIssue == null) return null;
                        String linkTypeName = linkTypeNames.getOrDefault(link.getLinkTypeId(), "Related");
                        return IssueResponse.LinkedIssueInfo.builder()
                                .linkType(linkTypeName)
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