package com.jira.admin.service;

import com.jira.admin.dto.IssueTypeSchemeResponse;
import com.jira.admin.dto.*;
import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Issue Administration Service - Issue types, priorities, statuses, workflows, screens
 */
@Service
@Slf4j
public class IssueAdministrationService {

    private final IssueTypeRepository issueTypeRepository;
    private final PriorityRepository priorityRepository;
    private final ResolutionRepository resolutionRepository;
    private final StatusRepository statusRepository;
    private final IssueTypeSchemeRepository issueTypeSchemeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCatalogSyncService projectCatalogSyncService;
    private final IssueSchemeBridgeService issueSchemeBridgeService;
    private final WorkflowRepository workflowRepository;
    private final WorkflowAdminProxyService workflowAdminProxyService;
    private final WorkflowSchemeAdminProxyService workflowSchemeAdminProxyService;
    private final WorkflowSchemeBridgeService workflowSchemeBridgeService;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final ScreenRepository screenRepository;
    private final ScreenSchemeRepository screenSchemeRepository;
    private final IssueTypeScreenSchemeRepository issueTypeScreenSchemeRepository;
    private final PermissionSchemeRepository permissionSchemeRepository;
    private final NotificationSchemeRepository notificationSchemeRepository;
    private final AuditLogRepository auditLogRepository;
    private final MessageSource messageSource;

    @Value("${app.defaults.status-color:#6C757D}")
    private String defaultStatusColor;

    @Value("${app.defaults.audit-severity:INFO}")
    private String defaultAuditSeverity;

    @Value("${app.defaults.audit-source:UI}")
    private String defaultAuditSource;

    public IssueAdministrationService(IssueTypeRepository issueTypeRepository,
                                       PriorityRepository priorityRepository,
                                       ResolutionRepository resolutionRepository,
                                       StatusRepository statusRepository,
                                       IssueTypeSchemeRepository issueTypeSchemeRepository,
                                       ProjectRepository projectRepository,
                                       ProjectCatalogSyncService projectCatalogSyncService,
                                       IssueSchemeBridgeService issueSchemeBridgeService,
                                       WorkflowRepository workflowRepository,
                                       WorkflowAdminProxyService workflowAdminProxyService,
                                       WorkflowSchemeAdminProxyService workflowSchemeAdminProxyService,
                                       WorkflowSchemeBridgeService workflowSchemeBridgeService,
                                       WorkflowSchemeRepository workflowSchemeRepository,
                                       ScreenRepository screenRepository,
                                       ScreenSchemeRepository screenSchemeRepository,
                                       IssueTypeScreenSchemeRepository issueTypeScreenSchemeRepository,
                                       PermissionSchemeRepository permissionSchemeRepository,
                                       NotificationSchemeRepository notificationSchemeRepository,
                                       AuditLogRepository auditLogRepository,
                                       MessageSource messageSource) {
        this.issueTypeRepository = issueTypeRepository;
        this.priorityRepository = priorityRepository;
        this.resolutionRepository = resolutionRepository;
        this.statusRepository = statusRepository;
        this.issueTypeSchemeRepository = issueTypeSchemeRepository;
        this.projectRepository = projectRepository;
        this.projectCatalogSyncService = projectCatalogSyncService;
        this.issueSchemeBridgeService = issueSchemeBridgeService;
        this.workflowRepository = workflowRepository;
        this.workflowAdminProxyService = workflowAdminProxyService;
        this.workflowSchemeAdminProxyService = workflowSchemeAdminProxyService;
        this.workflowSchemeBridgeService = workflowSchemeBridgeService;
        this.workflowSchemeRepository = workflowSchemeRepository;
        this.screenRepository = screenRepository;
        this.screenSchemeRepository = screenSchemeRepository;
        this.issueTypeScreenSchemeRepository = issueTypeScreenSchemeRepository;
        this.permissionSchemeRepository = permissionSchemeRepository;
        this.notificationSchemeRepository = notificationSchemeRepository;
        this.auditLogRepository = auditLogRepository;
        this.messageSource = messageSource;
    }

    // ==================== Issue Types ====================

    @Transactional(readOnly = true)
    public List<IssueTypeEntity> getIssueTypes() {
        return issueTypeRepository.findAll();
    }

    @Transactional
    public IssueTypeEntity createIssueType(Map<String, Object> data) {
        IssueTypeEntity issueType = IssueTypeEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .iconUrl((String) data.getOrDefault("iconUrl", ""))
                .issueTypeKey((String) data.getOrDefault("issueTypeKey", "standard"))
                .isSubtask((Boolean) data.getOrDefault("isSubtask", false))
                .isArchived(false)
                .build();

        issueType = issueTypeRepository.save(issueType);
        logAudit("CREATE", "ISSUE_TYPE", issueType.getId(), issueType.getName(), "Issue type created");

        return issueType;
    }

    @Transactional
    public IssueTypeEntity updateIssueType(String issueTypeId, Map<String, Object> updates) {
        IssueTypeEntity issueType = issueTypeRepository.findById(issueTypeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.issue.type.not.found", null, Locale.ENGLISH)));

        if (updates.containsKey("name")) issueType.setName((String) updates.get("name"));
        if (updates.containsKey("description")) issueType.setDescription((String) updates.get("description"));
        if (updates.containsKey("iconUrl")) issueType.setIconUrl((String) updates.get("iconUrl"));

        issueType = issueTypeRepository.save(issueType);
        logAudit("UPDATE", "ISSUE_TYPE", issueType.getId(), issueType.getName(), "Issue type updated");

        return issueType;
    }

    // ==================== Priorities ====================

    @Transactional(readOnly = true)
    public List<PriorityEntity> getPriorities() {
        return priorityRepository.findAll().stream()
                .sorted(Comparator.comparingInt(p -> p.getSequence() != null ? p.getSequence() : 0))
                .collect(Collectors.toList());
    }

    @Transactional
    public PriorityEntity createPriority(Map<String, Object> data) {
        String name = (String) data.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.priority.name.required", null, Locale.ENGLISH));
        }
        long count = priorityRepository.count();
        PriorityEntity priority = PriorityEntity.builder()
                .name(name)
                .description((String) data.getOrDefault("description", ""))
                .iconUrl((String) data.getOrDefault("iconUrl", ""))
                .statusColor((String) data.getOrDefault("statusColor", defaultStatusColor))
                .sequence((Integer) data.getOrDefault("sequence", (int) count + 1))
                .isDefault(count == 0)
                .build();

        priority = priorityRepository.save(priority);
        logAudit("CREATE", "PRIORITY", priority.getId(), priority.getName(), "Priority created");

        return priority;
    }

    @Transactional
    public PriorityEntity updatePriority(String priorityId, Map<String, Object> updates) {
        PriorityEntity priority = priorityRepository.findById(priorityId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.priority.not.found", null, Locale.ENGLISH)));

        if (updates.containsKey("name")) priority.setName((String) updates.get("name"));
        if (updates.containsKey("description")) priority.setDescription((String) updates.get("description"));
        if (updates.containsKey("iconUrl")) priority.setIconUrl((String) updates.get("iconUrl"));
        if (updates.containsKey("statusColor")) priority.setStatusColor((String) updates.get("statusColor"));
        if (updates.containsKey("sequence")) priority.setSequence((Integer) updates.get("sequence"));
        if (updates.containsKey("isDefault")) priority.setIsDefault((Boolean) updates.get("isDefault"));

        priority = priorityRepository.save(priority);
        logAudit("UPDATE", "PRIORITY", priority.getId(), priority.getName(), "Priority updated");

        return priority;
    }

    @Transactional
    public void deletePriority(String priorityId) {
        if (!priorityRepository.existsById(priorityId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.priority.not.found", null, Locale.ENGLISH));
        }
        priorityRepository.deleteById(priorityId);
        logAudit("DELETE", "PRIORITY", priorityId, null, "Priority deleted");
    }

    // ==================== Resolutions ====================

    @Transactional(readOnly = true)
    public List<ResolutionEntity> getResolutions() {
        return resolutionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(r -> r.getSequence() != null ? r.getSequence() : 0))
                .collect(Collectors.toList());
    }

    @Transactional
    public ResolutionEntity createResolution(Map<String, Object> data) {
        String name = (String) data.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.resolution.name.required", null, Locale.ENGLISH));
        }
        long count = resolutionRepository.count();
        ResolutionEntity resolution = ResolutionEntity.builder()
                .name(name)
                .description((String) data.getOrDefault("description", ""))
                .iconUrl((String) data.getOrDefault("iconUrl", ""))
                .sequence((Integer) data.getOrDefault("sequence", (int) count + 1))
                .isDefault(count == 0)
                .build();

        resolution = resolutionRepository.save(resolution);
        logAudit("CREATE", "RESOLUTION", resolution.getId(), resolution.getName(), "Resolution created");

        return resolution;
    }

    // ==================== Statuses ====================

    @Transactional(readOnly = true)
    public List<StatusEntity> getStatuses() {
        return statusRepository.findByIsArchivedFalseOrderBySequenceAsc();
    }

    @Transactional(readOnly = true)
    public StatusEntity getStatus(String statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.status.not.found", new Object[]{statusId}, Locale.ENGLISH)));
    }

    @Transactional
    public StatusEntity createStatus(CreateStatusRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.status.name.required", null, Locale.ENGLISH));
        }
        if (statusRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.status.name.exists", new Object[]{request.getName()}, Locale.ENGLISH));
        }

        long count = statusRepository.count();
        StatusEntity status = StatusEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .statusCategory(request.getStatusCategory() != null ? request.getStatusCategory() : StatusEntity.CATEGORY_TODO)
                .iconUrl(request.getIconUrl())
                .statusColor(request.getStatusColor() != null ? request.getStatusColor() : defaultStatusColor)
                .sequence(request.getSequence() != null ? request.getSequence() : (int) count + 1)
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : count == 0)
                .lookupGroup(request.getLookupGroup())
                .isActive(true)
                .isArchived(false)
                .build();

        status = statusRepository.save(status);
        logAudit("CREATE", "STATUS", status.getId(), status.getName(), "Status created");

        return status;
    }

    @Transactional
    public StatusEntity createStatus(Map<String, Object> data) {
        CreateStatusRequest request = CreateStatusRequest.builder()
                .name((String) data.get("name"))
                .description((String) data.get("description"))
                .statusCategory((String) data.get("statusCategory"))
                .statusColor((String) data.get("statusColor"))
                .iconUrl((String) data.get("iconUrl"))
                .sequence((Integer) data.get("sequence"))
                .isDefault((Boolean) data.get("isDefault"))
                .lookupGroup((String) data.get("lookupGroup"))
                .build();
        return createStatus(request);
    }

    @Transactional
    public StatusEntity updateStatus(String statusId, UpdateStatusRequest request) {
        StatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.status.not.found", new Object[]{statusId}, Locale.ENGLISH)));

        if (request.getName() != null && !request.getName().isEmpty()) {
            if (statusRepository.existsByNameAndIdNot(request.getName(), statusId)) {
                throw new IllegalArgumentException(
                        messageSource.getMessage("error.status.name.exists", new Object[]{request.getName()}, Locale.ENGLISH));
            }
            status.setName(request.getName());
        }
        if (request.getDescription() != null) status.setDescription(request.getDescription());
        if (request.getStatusCategory() != null) status.setStatusCategory(request.getStatusCategory());
        if (request.getIconUrl() != null) status.setIconUrl(request.getIconUrl());
        if (request.getStatusColor() != null) status.setStatusColor(request.getStatusColor());
        if (request.getSequence() != null) status.setSequence(request.getSequence());
        if (request.getIsDefault() != null) status.setIsDefault(request.getIsDefault());
        if (request.getIsActive() != null) status.setIsActive(request.getIsActive());
        if (request.getIsArchived() != null) status.setIsArchived(request.getIsArchived());
        if (request.getLookupGroup() != null) status.setLookupGroup(request.getLookupGroup());

        status = statusRepository.save(status);
        logAudit("UPDATE", "STATUS", status.getId(), status.getName(), "Status updated");

        return status;
    }

    @Transactional
    public StatusEntity updateStatus(String statusId, Map<String, Object> updates) {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .name((String) updates.get("name"))
                .description((String) updates.get("description"))
                .statusCategory((String) updates.get("statusCategory"))
                .statusColor((String) updates.get("statusColor"))
                .iconUrl((String) updates.get("iconUrl"))
                .sequence((Integer) updates.get("sequence"))
                .isDefault((Boolean) updates.get("isDefault"))
                .isActive((Boolean) updates.get("isActive"))
                .isArchived((Boolean) updates.get("isArchived"))
                .lookupGroup((String) updates.get("lookupGroup"))
                .build();
        return updateStatus(statusId, request);
    }

    @Transactional
    public void archiveStatus(String statusId) {
        StatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.status.not.found", new Object[]{statusId}, Locale.ENGLISH)));

        status.setIsArchived(true);
        status.setIsActive(false);
        statusRepository.save(status);
        logAudit("ARCHIVE", "STATUS", statusId, status.getName(), "Status archived");
    }

    @Transactional
    public void restoreStatus(String statusId) {
        StatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.status.not.found", new Object[]{statusId}, Locale.ENGLISH)));

        status.setIsArchived(false);
        status.setIsActive(true);
        statusRepository.save(status);
        logAudit("RESTORE", "STATUS", statusId, status.getName(), "Status restored");
    }

    @Transactional
    public void deleteStatus(String statusId) {
        StatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.status.not.found", new Object[]{statusId}, Locale.ENGLISH)));

        statusRepository.delete(status);
        logAudit("DELETE", "STATUS", statusId, status.getName(), "Status deleted");
    }

    @Transactional(readOnly = true)
    public List<StatusEntity> getStatusesByCategory(String category) {
        return statusRepository.findByStatusCategoryOrderBySequenceAsc(category);
    }

    // ==================== Issue Type Schemes ====================

    @Transactional(readOnly = true)
    public List<IssueTypeSchemeResponse> getIssueTypeSchemes() {
        return issueTypeSchemeRepository.findAll().stream()
                .map(this::toSchemeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IssueTypeSchemeResponse getIssueTypeScheme(String schemeId) {
        IssueTypeSchemeEntity scheme = issueTypeSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.issue.type.scheme.not.found", null, Locale.ENGLISH)));
        return toSchemeResponse(scheme);
    }

    @Transactional
    public IssueTypeSchemeResponse createIssueTypeScheme(Map<String, Object> data) {
        String name = (String) data.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.issue.type.name.required", null, Locale.ENGLISH));
        }

        IssueTypeSchemeEntity scheme = IssueTypeSchemeEntity.builder()
                .name(name)
                .description((String) data.getOrDefault("description", ""))
                .issueTypeIds(joinIssueTypeIds(data.get("issueTypeIds")))
                .defaultIssueType((String) data.get("defaultIssueType"))
                .isDefault(Boolean.TRUE.equals(data.get("isDefault")))
                .projectCount(0)
                .build();

        scheme = issueTypeSchemeRepository.save(scheme);
        logAudit("CREATE", "ISSUE_TYPE_SCHEME", scheme.getId(), scheme.getName(), "Issue type scheme created");

        return toSchemeResponse(scheme);
    }

    @Transactional
    public IssueTypeSchemeResponse updateIssueTypeScheme(String schemeId, Map<String, Object> data) {
        IssueTypeSchemeEntity scheme = issueTypeSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.issue.type.scheme.not.found", null, Locale.ENGLISH)));

        if (data.containsKey("name")) scheme.setName((String) data.get("name"));
        if (data.containsKey("description")) scheme.setDescription((String) data.get("description"));
        if (data.containsKey("issueTypeIds")) scheme.setIssueTypeIds(joinIssueTypeIds(data.get("issueTypeIds")));
        if (data.containsKey("defaultIssueType")) scheme.setDefaultIssueType((String) data.get("defaultIssueType"));
        if (data.containsKey("isDefault")) scheme.setIsDefault((Boolean) data.get("isDefault"));

        final IssueTypeSchemeEntity savedScheme = issueTypeSchemeRepository.save(scheme);
        scheme = savedScheme;
        logAudit("UPDATE", "ISSUE_TYPE_SCHEME", scheme.getId(), scheme.getName(), "Issue type scheme updated");

        List<String> assignedProjectIds = projectRepository.findAll().stream()
                .filter(p -> schemeId.equals(p.getIssueTypeScheme()) || savedScheme.getName().equals(p.getIssueTypeScheme()))
                .map(ProjectEntity::getId)
                .toList();
        if (!assignedProjectIds.isEmpty()) {
            issueSchemeBridgeService.pushSchemeToProjectService(scheme, assignedProjectIds);
        }

        return toSchemeResponse(scheme);
    }

    @Transactional
    public void deleteIssueTypeScheme(String schemeId) {
        IssueTypeSchemeEntity scheme = issueTypeSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.issue.type.scheme.not.found", null, Locale.ENGLISH)));

        long projects = projectRepository.countByIssueTypeScheme(scheme.getId());
        if (projects == 0) {
            projects = projectRepository.countByIssueTypeScheme(scheme.getName());
        }
        if (projects > 0) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.issue.type.scheme.in.use",
                            new Object[]{scheme.getName(), projects}, Locale.ENGLISH));
        }

        issueTypeSchemeRepository.delete(scheme);
        logAudit("DELETE", "ISSUE_TYPE_SCHEME", schemeId, scheme.getName(), "Issue type scheme deleted");
    }

    @Transactional(readOnly = true)
    public List<SchemeProjectAssignmentDto> getSchemeProjectAssignments(String schemeId) {
        IssueTypeSchemeEntity scheme = issueTypeSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.issue.type.scheme.not.found", null, Locale.ENGLISH)));

        if (projectRepository.count() == 0) {
            projectCatalogSyncService.syncFromProjectService();
        }

        return projectRepository.findAll().stream()
                .map(p -> toAssignmentDto(p, scheme))
                .sorted(Comparator.comparing(SchemeProjectAssignmentDto::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<SchemeProjectAssignmentDto> assignSchemeToProjects(String schemeId, List<String> projectIds) {
        IssueTypeSchemeEntity scheme = issueTypeSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.issue.type.scheme.not.found", null, Locale.ENGLISH)));

        if (projectRepository.count() == 0) {
            projectCatalogSyncService.syncFromProjectService();
        }

        Set<String> selected = projectIds == null
                ? Set.of()
                : projectIds.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());

        for (ProjectEntity project : projectRepository.findAll()) {
            boolean hasScheme = schemeId.equals(project.getIssueTypeScheme())
                    || scheme.getName().equals(project.getIssueTypeScheme());
            boolean shouldAssign = selected.contains(project.getId());

            if (shouldAssign) {
                project.setIssueTypeScheme(schemeId);
                projectRepository.save(project);
            } else if (hasScheme) {
                project.setIssueTypeScheme(null);
                projectRepository.save(project);
            }
        }

        if (!selected.isEmpty()) {
            issueSchemeBridgeService.pushSchemeToProjectService(scheme, new ArrayList<>(selected));
        }

        logAudit("UPDATE", "ISSUE_TYPE_SCHEME", schemeId, scheme.getName(),
                "Assigned scheme to " + selected.size() + " project(s)");

        return getSchemeProjectAssignments(schemeId);
    }

    private SchemeProjectAssignmentDto toAssignmentDto(ProjectEntity project, IssueTypeSchemeEntity scheme) {
        String currentId = project.getIssueTypeScheme();
        boolean assigned = scheme.getId().equals(currentId) || scheme.getName().equals(currentId);
        String currentName = null;
        if (currentId != null && !currentId.isBlank()) {
            currentName = issueTypeSchemeRepository.findById(currentId)
                    .map(IssueTypeSchemeEntity::getName)
                    .orElse(currentId);
        }
        return SchemeProjectAssignmentDto.builder()
                .id(project.getId())
                .projectKey(project.getProjectKey())
                .name(project.getName())
                .status(project.getStatus() != null ? project.getStatus().name() : "ACTIVE")
                .assigned(assigned)
                .currentSchemeId(currentId)
                .currentSchemeName(currentName)
                .build();
    }

    private IssueTypeSchemeResponse toSchemeResponse(IssueTypeSchemeEntity scheme) {
        long projects = projectRepository.countByIssueTypeScheme(scheme.getId());
        if (projects == 0) {
            projects = projectRepository.countByIssueTypeScheme(scheme.getName());
        }
        return IssueTypeSchemeResponse.builder()
                .id(scheme.getId())
                .name(scheme.getName())
                .description(scheme.getDescription())
                .defaultIssueType(scheme.getDefaultIssueType())
                .issueTypeIdList(parseIssueTypeIds(scheme.getIssueTypeIds()))
                .projectCount((int) projects)
                .isDefault(scheme.getIsDefault())
                .build();
    }

    private List<String> parseIssueTypeIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String joinIssueTypeIds(Object idsObj) {
        if (idsObj == null) {
            return "";
        }
        if (idsObj instanceof List<?> list) {
            return list.stream().map(Object::toString).map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }
        return idsObj.toString().trim();
    }

    // ==================== Workflows ====================

    @Transactional(readOnly = true)
    public List<WorkflowEntity> getWorkflows() {
        return workflowAdminProxyService.listWorkflows();
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowEntity> getWorkflowById(String workflowId) {
        return workflowAdminProxyService.getWorkflow(workflowId);
    }

    @Transactional
    public WorkflowEntity createWorkflow(Map<String, Object> data) {
        WorkflowEntity workflow = workflowAdminProxyService.createWorkflow(data);
        logAudit("CREATE", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow created (draft) via workflow-service");
        return workflow;
    }

    @Transactional
    public WorkflowEntity updateWorkflow(String workflowId, Map<String, Object> updates) {
        WorkflowEntity workflow = workflowAdminProxyService.updateWorkflow(workflowId, updates);
        logAudit("UPDATE", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow updated via workflow-service");
        return workflow;
    }

    @Transactional
    public WorkflowEntity publishWorkflow(String workflowId) {
        WorkflowEntity workflow = workflowAdminProxyService.publishWorkflow(workflowId);
        logAudit("PUBLISH", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow published via workflow-service");
        return workflow;
    }

    @Transactional
    public WorkflowEntity createDraftFromWorkflow(String workflowId) {
        WorkflowEntity draft = workflowAdminProxyService.createDraftFromWorkflow(workflowId);
        logAudit("CREATE_DRAFT", "WORKFLOW", draft.getId(), draft.getName(), "Draft created via workflow-service");
        return draft;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConditionDefinitions() {
        return workflowAdminProxyService.getConditionDefinitions();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getValidatorDefinitions() {
        return workflowAdminProxyService.getValidatorDefinitions();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostFunctionDefinitions() {
        return workflowAdminProxyService.getPostFunctionDefinitions();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listWorkflowScreens(String screenType) {
        return workflowAdminProxyService.listScreens(screenType);
    }

    // ==================== Workflow Schemes ====================

    @Transactional(readOnly = true)
    public List<WorkflowSchemeEntity> getWorkflowSchemes() {
        return workflowSchemeAdminProxyService.listSchemes();
    }

    @Transactional
    public WorkflowSchemeEntity createWorkflowScheme(Map<String, Object> data) {
        WorkflowSchemeEntity scheme = workflowSchemeAdminProxyService.createScheme(data);
        logAudit("CREATE", "WORKFLOW_SCHEME", scheme.getId(), scheme.getName(), "Workflow scheme created via workflow-service");

        @SuppressWarnings("unchecked")
        List<String> projectIds = (List<String>) data.get("projectIds");
        if (projectIds != null && !projectIds.isEmpty()) {
            workflowSchemeBridgeService.pushSchemeToProjects(scheme.getId(), projectIds);
        }

        return scheme;
    }

    // ==================== Screens ====================

    @Transactional(readOnly = true)
    public List<ScreenEntity> getScreens() {
        return screenRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getScreenFields(String screenId) {
        ScreenEntity screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.screen.not.found", new Object[]{screenId}, Locale.ENGLISH)));
        List<Map<String, Object>> tabs = new ArrayList<>();
        List<String> allFieldIds = new ArrayList<>();
        if (screen.getTabs() != null) {
            for (ScreenTab tab : screen.getTabs()) {
                List<String> fieldIds = parseFieldIds(tab.getFieldIds());
                allFieldIds.addAll(fieldIds);
                tabs.add(Map.of(
                        "tabName", tab.getTabName() != null ? tab.getTabName() : "Tab",
                        "fieldIds", fieldIds));
            }
        }
        return Map.of(
                "screenId", screen.getId(),
                "screenName", screen.getName(),
                "tabs", tabs,
                "fieldIds", allFieldIds.stream().distinct().toList());
    }

    private List<String> parseFieldIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("[")) {
                return Arrays.stream(trimmed.replace("[", "").replace("]", "").split(","))
                        .map(s -> s.trim().replace("\"", ""))
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        } catch (Exception ignored) {
        }
        return Arrays.stream(trimmed.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Transactional
    public ScreenEntity createScreen(Map<String, Object> data) {
        ScreenEntity screen = ScreenEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .tabs(new ArrayList<>())
                .isDefault(false)
                .build();

        screen = screenRepository.save(screen);
        logAudit("CREATE", "SCREEN", screen.getId(), screen.getName(), "Screen created");

        return screen;
    }

    @Transactional
    public ScreenEntity addScreenTab(String screenId, String tabName) {
        ScreenEntity screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.screen.not.found.simple", null, Locale.ENGLISH)));

        ScreenTab tab = ScreenTab.builder()
                .screen(screen)
                .tabName(tabName)
                .tabOrder(screen.getTabs().size())
                .fieldIds("")
                .build();

        screen.getTabs().add(tab);
        screen = screenRepository.save(screen);
        logAudit("ADD_TAB", "SCREEN", screenId, screen.getName(), "Tab '" + tabName + "' added to screen");

        return screen;
    }

    @Transactional
    public ScreenEntity addFieldToTab(String screenId, int tabIndex, String fieldId) {
        ScreenEntity screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.screen.not.found.simple", null, Locale.ENGLISH)));

        if (tabIndex >= 0 && tabIndex < screen.getTabs().size()) {
            ScreenTab tab = screen.getTabs().get(tabIndex);
            tab.setFieldIds(tab.getFieldIds() + (tab.getFieldIds().isEmpty() ? fieldId : "," + fieldId));
            screen = screenRepository.save(screen);
            logAudit("ADD_FIELD", "SCREEN", screenId, screen.getName(), "Field added to tab");
        }

        return screen;
    }

    // ==================== Screen Schemes ====================

    @Transactional(readOnly = true)
    public List<ScreenSchemeEntity> getScreenSchemes() {
        return screenSchemeRepository.findAll();
    }

    @Transactional
    public ScreenSchemeEntity createScreenScheme(Map<String, Object> data) {
        ScreenSchemeEntity scheme = ScreenSchemeEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .createScreenId((String) data.get("createScreenId"))
                .editScreenId((String) data.get("editScreenId"))
                .viewScreenId((String) data.get("viewScreenId"))
                .isDefault(false)
                .build();

        scheme = screenSchemeRepository.save(scheme);
        logAudit("CREATE", "SCREEN_SCHEME", scheme.getId(), scheme.getName(), "Screen scheme created");

        return scheme;
    }

    // ==================== Issue Type Screen Schemes ====================

    @Transactional(readOnly = true)
    public List<IssueTypeScreenSchemeEntity> getIssueTypeScreenSchemes() {
        return issueTypeScreenSchemeRepository.findAll();
    }

    // ==================== Permission Schemes ====================

    @Transactional(readOnly = true)
    public List<PermissionSchemeEntity> getPermissionSchemes() {
        return permissionSchemeRepository.findAll();
    }

    @Transactional
    public PermissionSchemeEntity createPermissionScheme(Map<String, Object> data) {
        PermissionSchemeEntity scheme = PermissionSchemeEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .permissions("[]")
                .isDefault(false)
                .build();

        scheme = permissionSchemeRepository.save(scheme);
        logAudit("CREATE", "PERMISSION_SCHEME", scheme.getId(), scheme.getName(), "Permission scheme created");

        return scheme;
    }

    // ==================== Notification Schemes ====================

    @Transactional(readOnly = true)
    public List<NotificationSchemeEntity> getNotificationSchemes() {
        return notificationSchemeRepository.findAll();
    }

    @Transactional
    public NotificationSchemeEntity createNotificationScheme(Map<String, Object> data) {
        NotificationSchemeEntity scheme = NotificationSchemeEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .isDefault(false)
                .build();

        scheme = notificationSchemeRepository.save(scheme);
        logAudit("CREATE", "NOTIFICATION_SCHEME", scheme.getId(), scheme.getName(), "Notification scheme created");

        return scheme;
    }

    // ==================== Helper Methods ====================

    private void logAudit(String action, String category, String entityId, String entityName, String details) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .timestamp(LocalDateTime.now())
                .action(action)
                .category(category)
                .entityType(category)
                .entityId(entityId)
                .entityName(entityName)
                .details(details)
                .result("SUCCESS")
                .severity(defaultAuditSeverity)
                .source(defaultAuditSource)
                .build();
        auditLogRepository.save(auditLog);
    }
}
