package com.jira.component.service;

import com.jira.component.dto.*;
import com.jira.component.entity.*;
import com.jira.component.exception.*;
import com.jira.component.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComponentService {

    private final ProjectComponentRepository componentRepository;
    private final IssueComponentRepository issueComponentRepository;
    private final ComponentAuditLogRepository auditLogRepository;
    private final ComponentOwnershipHistoryRepository ownershipHistoryRepository;
    private final ComponentMetricsRepository metricsRepository;
    private final ComponentAssignmentRuleRepository assignmentRuleRepository;

    // ========== COMPONENT CRUD ==========

    @Transactional(readOnly = true)
    public List<ComponentResponse> getComponentsByProject(UUID projectId, boolean includeArchived) {
        List<ProjectComponent> components;
        if (includeArchived) {
            components = componentRepository.findByProjectIdAndDeletedFalseOrderBySequenceAsc(projectId);
        } else {
            components = componentRepository.findActiveByProjectId(projectId);
        }
        return components.stream()
            .map(this::toComponentResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComponentResponse getComponentById(UUID componentId) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));
        return toComponentResponse(component);
    }

    @Transactional
    public ComponentResponse createComponent(CreateComponentRequest request) {
        // Check for duplicate name
        if (componentRepository.existsByProjectIdAndNameAndIdNot(request.getProjectId(), request.getName(), null)) {
            throw new DuplicateResourceException("Component with name '" + request.getName() + "' already exists in this project");
        }

        int nextSequence = (int) componentRepository.countByProjectId(request.getProjectId());

        ProjectComponent component = ProjectComponent.builder()
            .projectId(request.getProjectId())
            .name(request.getName())
            .description(request.getDescription())
            .leadUserId(request.getLeadUserId())
            .assigneeType(request.getAssigneeType() != null ? request.getAssigneeType() : "PROJECT_DEFAULT")
            .defaultAssignee(request.getDefaultAssignee())
            .color(request.getColor())
            .icon(request.getIcon())
            .sequence(nextSequence)
            .archived(false)
            .deleted(false)
            .build();

        component = componentRepository.save(component);
        createAuditLog(component.getId(), "CREATED", null, null, "Component created");

        log.info("Created component: {} for project: {}", component.getName(), component.getProjectId());
        return toComponentResponse(component);
    }

    @Transactional
    public ComponentResponse updateComponent(UUID componentId, UpdateComponentRequest request, UUID userId) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        // Check for duplicate name
        if (request.getName() != null && !request.getName().equals(component.getName())) {
            if (componentRepository.existsByProjectIdAndNameAndIdNot(component.getProjectId(), request.getName(), componentId)) {
                throw new DuplicateResourceException("Component with name '" + request.getName() + "' already exists in this project");
            }
        }

        // Track changes for audit
        if (request.getName() != null && !request.getName().equals(component.getName())) {
            createAuditLog(componentId, "UPDATED", "name", component.getName(), request.getName(), userId);
        }
        if (request.getLeadUserId() != null && !request.getLeadUserId().equals(component.getLeadUserId())) {
            createAuditLog(componentId, "UPDATED", "leadUserId",
                component.getLeadUserId() != null ? component.getLeadUserId().toString() : null,
                request.getLeadUserId().toString(), userId);
        }

        // Update fields
        if (request.getName() != null) component.setName(request.getName());
        if (request.getDescription() != null) component.setDescription(request.getDescription());
        if (request.getLeadUserId() != null) component.setLeadUserId(request.getLeadUserId());
        if (request.getAssigneeType() != null) component.setAssigneeType(request.getAssigneeType());
        if (request.getDefaultAssignee() != null) component.setDefaultAssignee(request.getDefaultAssignee());
        if (request.getColor() != null) component.setColor(request.getColor());
        if (request.getIcon() != null) component.setIcon(request.getIcon());
        if (request.getSequence() != null) component.setSequence(request.getSequence());

        component = componentRepository.save(component);
        log.info("Updated component: {}", componentId);

        return toComponentResponse(component);
    }

    @Transactional
    public void deleteComponent(UUID componentId) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        component.setDeleted(true);
        componentRepository.save(component);

        createAuditLog(componentId, "DELETED", null, null, "Component deleted");
        log.info("Deleted component: {}", componentId);
    }

    // ========== ARCHIVE OPERATIONS ==========

    @Transactional
    public ComponentResponse archiveComponent(UUID componentId, UUID userId) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        component.setArchived(true);
        component = componentRepository.save(component);

        createAuditLog(componentId, "ARCHIVED", null, null, "Component archived", userId);
        log.info("Archived component: {}", componentId);

        return toComponentResponse(component);
    }

    @Transactional
    public ComponentResponse unarchiveComponent(UUID componentId, UUID userId) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        component.setArchived(false);
        component = componentRepository.save(component);

        createAuditLog(componentId, "UNARCHIVED", null, null, "Component unarchived", userId);
        log.info("Unarchived component: {}", componentId);

        return toComponentResponse(component);
    }

    @Transactional
    public ComponentResponse restoreComponent(UUID componentId) {
        ProjectComponent component = componentRepository.findById(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        component.setDeleted(false);
        component = componentRepository.save(component);

        createAuditLog(componentId, "RESTORED", null, null, "Component restored");
        log.info("Restored component: {}", componentId);

        return toComponentResponse(component);
    }

    // ========== ISSUE COMPONENT LINKING ==========

    @Transactional
    public void assignComponent(UUID issueId, UUID componentId, UUID userId) {
        if (issueComponentRepository.existsByIssueIdAndComponentId(issueId, componentId)) {
            return;
        }

        IssueComponent issueComponent = IssueComponent.builder()
            .issueId(issueId)
            .componentId(componentId)
            .createdBy(userId)
            .build();

        issueComponentRepository.save(issueComponent);
        createAuditLog(componentId, "ISSUE_ADDED", "issueId", null, "Issue " + issueId + " added to component", userId);

        log.info("Assigned component {} to issue {}", componentId, issueId);
    }

    @Transactional
    public void removeComponent(UUID issueId, UUID componentId) {
        List<IssueComponent> links = issueComponentRepository.findByIssueId(issueId);
        links.stream()
            .filter(l -> l.getComponentId().equals(componentId))
            .findFirst()
            .ifPresent(issueComponentRepository::delete);

        createAuditLog(componentId, "ISSUE_REMOVED", "issueId", issueId.toString(), "Issue removed from component");
    }

    @Transactional(readOnly = true)
    public List<UUID> getIssueComponents(UUID issueId) {
        return issueComponentRepository.findByIssueId(issueId).stream()
            .map(IssueComponent::getComponentId)
            .collect(Collectors.toList());
    }

    // ========== BULK OPERATIONS ==========

    @Transactional
    public int bulkAssignComponent(List<UUID> issueIds, UUID componentId, UUID userId) {
        int count = 0;
        for (UUID issueId : issueIds) {
            if (!issueComponentRepository.existsByIssueIdAndComponentId(issueId, componentId)) {
                IssueComponent issueComponent = IssueComponent.builder()
                    .issueId(issueId)
                    .componentId(componentId)
                    .createdBy(userId)
                    .build();
                issueComponentRepository.save(issueComponent);
                count++;
            }
        }
        createAuditLog(componentId, "BULK_ASSIGN", "issueCount", null, count + " issues assigned to component", userId);
        log.info("Bulk assigned {} issues to component {}", count, componentId);
        return count;
    }

    @Transactional
    public int bulkRemoveComponent(List<UUID> issueIds, UUID componentId) {
        int[] count = {0};
        for (UUID issueId : issueIds) {
            List<IssueComponent> links = issueComponentRepository.findByIssueId(issueId);
            Optional<IssueComponent> link = links.stream()
                .filter(l -> l.getComponentId().equals(componentId))
                .findFirst();
            if (link.isPresent()) {
                issueComponentRepository.delete(link.get());
                count[0]++;
            }
        }
        createAuditLog(componentId, "BULK_REMOVE", "issueCount", null, count[0] + " issues removed from component");
        log.info("Bulk removed {} issues from component {}", count[0], componentId);
        return count[0];
    }

    // ========== OWNERSHIP TRANSFER ==========

    @Transactional
    public ComponentResponse transferOwnership(UUID componentId, TransferOwnershipRequest request) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        // Record ownership history
        ComponentOwnershipHistory history = ComponentOwnershipHistory.builder()
            .componentId(componentId)
            .previousLeadId(component.getLeadUserId())
            .newLeadId(request.getNewLeadId())
            .transferReason(request.getReason())
            .transferredBy(request.getTransferredBy())
            .build();

        ownershipHistoryRepository.save(history);

        // Update component
        component.setLeadUserId(request.getNewLeadId());
        component = componentRepository.save(component);

        createAuditLog(componentId, "OWNERSHIP_TRANSFERRED",
            "leadUserId",
            component.getLeadUserId() != null ? component.getLeadUserId().toString() : null,
            request.getNewLeadId().toString(),
            request.getTransferredBy());

        log.info("Transferred ownership of component {} to user {}", componentId, request.getNewLeadId());
        return toComponentResponse(component);
    }

    @Transactional(readOnly = true)
    public List<OwnershipTransferResponse> getOwnershipHistory(UUID componentId) {
        return ownershipHistoryRepository.findByComponentIdOrderByTransferredAtDesc(componentId).stream()
            .map(this::toOwnershipTransferResponse)
            .collect(Collectors.toList());
    }

    // ========== METRICS ==========

    @Transactional
    public List<ComponentMetricsResponse> getComponentMetrics(UUID componentId) {
        return metricsRepository.findByComponentIdOrderBySnapshotDateAsc(componentId).stream()
            .map(this::toMetricsResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ComponentMetricsResponse recordMetricsSnapshot(UUID componentId) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        long totalCount = issueComponentRepository.countByComponentId(componentId);

        ComponentMetrics metrics = ComponentMetrics.builder()
            .componentId(componentId)
            .snapshotDate(java.time.LocalDate.now())
            .totalIssues((int) totalCount)
            .build();

        metrics = metricsRepository.save(metrics);
        return toMetricsResponse(metrics);
    }

    // ========== ASSIGNMENT RULES ==========

    @Transactional
    public List<ComponentAssignmentRuleResponse> getAssignmentRules(UUID componentId) {
        return assignmentRuleRepository.findByComponentId(componentId).stream()
            .map(this::toAssignmentRuleResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ComponentAssignmentRuleResponse createAssignmentRule(UUID componentId, ComponentAssignmentRule rule, UUID userId) {
        ProjectComponent component = componentRepository.findByIdAndDeletedFalse(componentId)
            .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        rule.setComponentId(componentId);
        rule.setCreatedBy(userId);
        rule = assignmentRuleRepository.save(rule);

        createAuditLog(componentId, "ASSIGNMENT_RULE_CREATED", "ruleType", null, "Assignment rule created", userId);

        return toAssignmentRuleResponse(rule);
    }

    @Transactional
    public void deleteAssignmentRule(UUID ruleId) {
        assignmentRuleRepository.deleteById(ruleId);
    }

    @Transactional(readOnly = true)
    public UUID resolveAutoAssignment(UUID componentId, UUID issueTypeId, UUID priorityId) {
        List<ComponentAssignmentRule> rules = assignmentRuleRepository.findByComponentIdAndIsActiveTrue(componentId);

        for (ComponentAssignmentRule rule : rules) {
            boolean matchIssueType = rule.getIssueTypeId() == null || rule.getIssueTypeId().equals(issueTypeId);
            boolean matchPriority = rule.getPriorityId() == null || rule.getPriorityId().equals(priorityId);

            if (matchIssueType && matchPriority) {
                if (rule.getAssigneeId() != null) {
                    return rule.getAssigneeId();
                }
            }
        }

        return null;
    }

    // ========== AUDIT ==========

    @Transactional(readOnly = true)
    public List<ComponentAuditLog> getComponentAuditLogs(UUID componentId) {
        return auditLogRepository.findByComponentIdOrderByCreatedAtDesc(componentId);
    }

    private void createAuditLog(UUID componentId, String action, String fieldName, String oldValue, String newValue, UUID userId) {
        if (componentId == null) return;

        ComponentAuditLog auditLog = ComponentAuditLog.builder()
            .componentId(componentId)
            .action(action)
            .fieldName(fieldName)
            .oldValue(oldValue)
            .newValue(newValue)
            .userId(userId)
            .build();

        auditLogRepository.save(auditLog);
    }

    private void createAuditLog(UUID componentId, String action, String fieldName, String oldValue, String newValue) {
        createAuditLog(componentId, action, fieldName, oldValue, newValue, null);
    }

    // ========== HELPERS ==========

    private ComponentResponse toComponentResponse(ProjectComponent component) {
        long issueCount = issueComponentRepository.countByComponentId(component.getId());

        return ComponentResponse.builder()
            .id(component.getId())
            .projectId(component.getProjectId())
            .name(component.getName())
            .description(component.getDescription())
            .leadUserId(component.getLeadUserId())
            .assigneeType(component.getAssigneeType())
            .defaultAssignee(component.getDefaultAssignee())
            .archived(component.getArchived())
            .color(component.getColor())
            .icon(component.getIcon())
            .sequence(component.getSequence())
            .createdBy(component.getCreatedBy())
            .updatedBy(component.getUpdatedBy())
            .createdAt(component.getCreatedAt())
            .updatedAt(component.getUpdatedAt())
            .issueCount(issueCount)
            .build();
    }

    private ComponentMetricsResponse toMetricsResponse(ComponentMetrics metrics) {
        return ComponentMetricsResponse.builder()
            .componentId(metrics.getComponentId())
            .snapshotDate(metrics.getSnapshotDate())
            .totalIssues(metrics.getTotalIssues())
            .openIssues(metrics.getOpenIssues())
            .closedIssues(metrics.getClosedIssues())
            .bugCount(metrics.getBugCount())
            .storyCount(metrics.getStoryCount())
            .taskCount(metrics.getTaskCount())
            .totalStoryPoints(metrics.getTotalStoryPoints() != null ? metrics.getTotalStoryPoints().doubleValue() : 0)
            .completedStoryPoints(metrics.getCompletedStoryPoints() != null ? metrics.getCompletedStoryPoints().doubleValue() : 0)
            .avgResolutionTimeHours(metrics.getAvgResolutionTimeHours() != null ? metrics.getAvgResolutionTimeHours().doubleValue() : 0)
            .build();
    }

    private ComponentAssignmentRuleResponse toAssignmentRuleResponse(ComponentAssignmentRule rule) {
        return ComponentAssignmentRuleResponse.builder()
            .id(rule.getId())
            .componentId(rule.getComponentId())
            .ruleType(rule.getRuleType())
            .issueTypeId(rule.getIssueTypeId())
            .priorityId(rule.getPriorityId())
            .assigneeType(rule.getAssigneeType())
            .assigneeId(rule.getAssigneeId())
            .isActive(rule.getIsActive())
            .build();
    }

    private OwnershipTransferResponse toOwnershipTransferResponse(ComponentOwnershipHistory history) {
        return OwnershipTransferResponse.builder()
            .id(history.getId())
            .componentId(history.getComponentId())
            .previousLeadId(history.getPreviousLeadId())
            .newLeadId(history.getNewLeadId())
            .transferReason(history.getTransferReason())
            .transferredBy(history.getTransferredBy())
            .transferredAt(history.getTransferredAt())
            .build();
    }
}