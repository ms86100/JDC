package com.jira.admin.service;

import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Issue Administration Service - Issue types, priorities, statuses, workflows, screens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueAdministrationService {

    private final IssueTypeRepository issueTypeRepository;
    private final PriorityRepository priorityRepository;
    private final ResolutionRepository resolutionRepository;
    private final StatusRepository statusRepository;
    private final IssueTypeSchemeRepository issueTypeSchemeRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final ScreenRepository screenRepository;
    private final ScreenSchemeRepository screenSchemeRepository;
    private final IssueTypeScreenSchemeRepository issueTypeScreenSchemeRepository;
    private final PermissionSchemeRepository permissionSchemeRepository;
    private final NotificationSchemeRepository notificationSchemeRepository;
    private final AuditLogRepository auditLogRepository;

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
                .orElseThrow(() -> new IllegalArgumentException("Issue type not found"));

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
        long count = priorityRepository.count();
        PriorityEntity priority = PriorityEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .iconUrl((String) data.getOrDefault("iconUrl", ""))
                .statusColor((String) data.getOrDefault("statusColor", "#6C757D"))
                .sequence((Integer) data.getOrDefault("sequence", (int) count + 1))
                .isDefault(count == 0)
                .build();

        priority = priorityRepository.save(priority);
        logAudit("CREATE", "PRIORITY", priority.getId(), priority.getName(), "Priority created");

        return priority;
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
        long count = resolutionRepository.count();
        ResolutionEntity resolution = ResolutionEntity.builder()
                .name((String) data.get("name"))
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
        return statusRepository.findAll().stream()
                .sorted(Comparator.comparingInt(s -> s.getSequence() != null ? s.getSequence() : 0))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StatusEntity getStatus(String statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status not found: " + statusId));
    }

    @Transactional
    public StatusEntity createStatus(Map<String, Object> data) {
        long count = statusRepository.count();
        StatusEntity status = StatusEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .statusCategory((String) data.getOrDefault("statusCategory", "TODO"))
                .iconUrl((String) data.getOrDefault("iconUrl", ""))
                .statusColor((String) data.getOrDefault("statusColor", "#6C757D"))
                .sequence((Integer) data.getOrDefault("sequence", (int) count + 1))
                .build();

        status = statusRepository.save(status);
        logAudit("CREATE", "STATUS", status.getId(), status.getName(), "Status created");

        return status;
    }

    @Transactional
    public StatusEntity updateStatus(String statusId, Map<String, Object> updates) {
        StatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status not found: " + statusId));

        if (updates.containsKey("name")) status.setName((String) updates.get("name"));
        if (updates.containsKey("description")) status.setDescription((String) updates.get("description"));
        if (updates.containsKey("statusCategory")) status.setStatusCategory((String) updates.get("statusCategory"));
        if (updates.containsKey("iconUrl")) status.setIconUrl((String) updates.get("iconUrl"));
        if (updates.containsKey("statusColor")) status.setStatusColor((String) updates.get("statusColor"));
        if (updates.containsKey("sequence")) status.setSequence((Integer) updates.get("sequence"));

        status = statusRepository.save(status);
        logAudit("UPDATE", "STATUS", status.getId(), status.getName(), "Status updated");

        return status;
    }

    @Transactional
    public void deleteStatus(String statusId) {
        StatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status not found: " + statusId));

        statusRepository.delete(status);
        logAudit("DELETE", "STATUS", statusId, status.getName(), "Status deleted");
    }

    // ==================== Issue Type Schemes ====================

    @Transactional(readOnly = true)
    public List<IssueTypeSchemeEntity> getIssueTypeSchemes() {
        return issueTypeSchemeRepository.findAll();
    }

    @Transactional
    public IssueTypeSchemeEntity createIssueTypeScheme(Map<String, Object> data) {
        Object idsObj = data.getOrDefault("issueTypeIds", new ArrayList<>());
        String ids = idsObj instanceof List ? String.join(",", (List<String>) idsObj) : idsObj.toString();

        IssueTypeSchemeEntity scheme = IssueTypeSchemeEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .issueTypeIds(ids)
                .isDefault(false)
                .build();

        scheme = issueTypeSchemeRepository.save(scheme);
        logAudit("CREATE", "ISSUE_TYPE_SCHEME", scheme.getId(), scheme.getName(), "Issue type scheme created");

        return scheme;
    }

    // ==================== Workflows ====================

    @Transactional(readOnly = true)
    public List<WorkflowEntity> getWorkflows() {
        return workflowRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowEntity> getWorkflowById(String workflowId) {
        return workflowRepository.findById(workflowId);
    }

    @Transactional
    public WorkflowEntity createWorkflow(Map<String, Object> data) {
        WorkflowEntity workflow = WorkflowEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .workflowContent((String) data.getOrDefault("workflowContent", "{}"))
                .isSystem(false)
                .isActive(false)
                .isDraft(true)
                .version(1)
                .build();

        workflow = workflowRepository.save(workflow);
        logAudit("CREATE", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow created (draft)");

        return workflow;
    }

    @Transactional
    public WorkflowEntity updateWorkflow(String workflowId, Map<String, Object> updates) {
        WorkflowEntity workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        if (updates.containsKey("name")) workflow.setName((String) updates.get("name"));
        if (updates.containsKey("description")) workflow.setDescription((String) updates.get("description"));
        if (updates.containsKey("workflowContent")) {
            workflow.setWorkflowContent((String) updates.get("workflowContent"));
        }

        workflow = workflowRepository.save(workflow);
        logAudit("UPDATE", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow updated");

        return workflow;
    }

    @Transactional
    public WorkflowEntity publishWorkflow(String workflowId) {
        WorkflowEntity workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        workflow.setIsDraft(false);
        workflow.setIsActive(true);
        workflow.setVersion(workflow.getVersion() != null ? workflow.getVersion() + 1 : 1);

        workflow = workflowRepository.save(workflow);
        logAudit("PUBLISH", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow published");

        return workflow;
    }

    @Transactional
    public WorkflowEntity createDraftFromWorkflow(String workflowId) {
        WorkflowEntity original = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        WorkflowEntity draft = WorkflowEntity.builder()
                .name(original.getName() + " (Draft)")
                .description(original.getDescription())
                .workflowContent(original.getWorkflowContent())
                .isSystem(false)
                .isActive(false)
                .isDraft(true)
                .version(original.getVersion())
                .build();

        draft = workflowRepository.save(draft);
        logAudit("CREATE_DRAFT", "WORKFLOW", draft.getId(), draft.getName(), "Draft created from workflow " + original.getName());

        return draft;
    }

    // ==================== Workflow Schemes ====================

    @Transactional(readOnly = true)
    public List<WorkflowSchemeEntity> getWorkflowSchemes() {
        return workflowSchemeRepository.findAll();
    }

    @Transactional
    public WorkflowSchemeEntity createWorkflowScheme(Map<String, Object> data) {
        WorkflowSchemeEntity scheme = WorkflowSchemeEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.getOrDefault("description", ""))
                .defaultWorkflowId((String) data.get("defaultWorkflowId"))
                .build();

        scheme = workflowSchemeRepository.save(scheme);
        logAudit("CREATE", "WORKFLOW_SCHEME", scheme.getId(), scheme.getName(), "Workflow scheme created");

        return scheme;
    }

    // ==================== Screens ====================

    @Transactional(readOnly = true)
    public List<ScreenEntity> getScreens() {
        return screenRepository.findAll();
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
                .orElseThrow(() -> new IllegalArgumentException("Screen not found"));

        ScreenTab tab = ScreenTab.builder()
                .tabName(tabName)
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
                .orElseThrow(() -> new IllegalArgumentException("Screen not found"));

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
                .severity("INFO")
                .source("UI")
                .build();
        auditLogRepository.save(auditLog);
    }
}