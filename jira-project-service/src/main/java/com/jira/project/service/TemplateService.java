package com.jira.project.service;

import com.jira.project.dto.*;
import com.jira.project.entity.*;
import com.jira.project.exception.ResourceNotFoundException;
import com.jira.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TemplateService - Handles template CRUD operations and workflow associations
 *
 * This service provides:
 * - Template listing with workflow visualization data
 * - Template details with full workflow preview
 * - Template creation and updates
 * - Template issue type management
 * - Template workflow status and transition management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TemplateService {

    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectTypeRepository projectTypeRepository;
    private final TemplateWorkflowStatusRepository templateWorkflowStatusRepository;
    private final TemplateWorkflowTransitionRepository templateWorkflowTransitionRepository;
    private final TemplateIssueTypeRepository templateIssueTypeRepository;
    private final TemplateSchemeMappingRepository templateSchemeMappingRepository;
    private final StatusDefinitionRepository statusDefinitionRepository;

    /**
     * Get all active templates grouped by category
     */
    public List<TemplateCategoryResponse> getTemplatesByCategory() {
        log.debug("Fetching templates grouped by category");

        List<ProjectTemplate> templates = projectTemplateRepository.findAll().stream()
                .filter(ProjectTemplate::getIsActive)
                .collect(Collectors.toList());

        // Group by category
        Map<String, List<ProjectTemplate>> byCategory = templates.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "SOFTWARE"
                ));

        return byCategory.entrySet().stream()
                .map(entry -> TemplateCategoryResponse.builder()
                        .categoryName(entry.getKey())
                        .categoryIcon(getCategoryIcon(entry.getKey()))
                        .templates(entry.getValue().stream()
                                .map(this::mapToTemplateResponse)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get all templates for a specific category
     */
    public List<ProjectTemplateResponse> getTemplatesByType(UUID typeId) {
        log.debug("Fetching templates for type: {}", typeId);
        return projectTemplateRepository.findByTypeIdAndIsActiveTrueOrderBySortOrderAsc(typeId).stream()
                .map(this::mapToTemplateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get templates for a specific category
     */
    public List<ProjectTemplateResponse> getTemplatesByCategory(String category) {
        log.debug("Fetching templates for category: {}", category);
        return projectTemplateRepository.findAll().stream()
                .filter(t -> t.getIsActive() &&
                             category.equalsIgnoreCase(t.getCategory()))
                .map(this::mapToTemplateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get template with full workflow details
     */
    public TemplateWithWorkflowResponse getTemplateWithWorkflow(UUID templateId) {
        log.debug("Fetching template with workflow details: {}", templateId);

        ProjectTemplate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));

        ProjectType type = template.getType();

        // Get issue types
        List<TemplateIssueType> issueTypes = templateIssueTypeRepository.findByTemplateIdOrderBySequenceAsc(templateId);

        // Get workflow statuses
        List<TemplateWorkflowStatus> statuses = templateWorkflowStatusRepository.findByTemplateIdOrderBySequenceAsc(templateId);

        // Get workflow transitions
        List<TemplateWorkflowTransition> transitions = templateWorkflowTransitionRepository.findByTemplateIdOrderBySequenceAsc(templateId);

        // Get scheme mappings
        List<TemplateSchemeMapping> schemeMappings = templateSchemeMappingRepository.findByTemplateId(templateId);

        // Build scheme info DTOs
        TemplateWithWorkflowResponse.SchemeInfoDto issueTypeSchemeInfo = buildSchemeInfo(schemeMappings, "ISSUE_TYPE");
        TemplateWithWorkflowResponse.SchemeInfoDto workflowSchemeInfo = buildSchemeInfo(schemeMappings, "WORKFLOW");
        TemplateWithWorkflowResponse.SchemeInfoDto permissionSchemeInfo = buildSchemeInfo(schemeMappings, "PERMISSION");
        TemplateWithWorkflowResponse.SchemeInfoDto notificationSchemeInfo = buildSchemeInfo(schemeMappings, "NOTIFICATION");
        TemplateWithWorkflowResponse.SchemeInfoDto screenSchemeInfo = buildSchemeInfo(schemeMappings, "SCREEN");

        return TemplateWithWorkflowResponse.builder()
                .id(template.getId())
                .typeId(type.getId())
                .typeName(type.getName())
                .name(template.getName())
                .description(template.getDescription())
                .icon(template.getIcon())
                .color(template.getColor())
                .category(template.getCategory())
                .templateType(template.getTemplateType())
                .defaultAssigneeType(template.getDefaultAssigneeType())
                .allowIssueCreation(template.getAllowIssueCreation())
                .sortOrder(template.getSortOrder())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .instructions(template.getInstructions())
                .issueTypes(issueTypes.stream()
                        .map(this::mapToWorkflowIssueTypeDto)
                        .collect(Collectors.toList()))
                .workflowStatuses(statuses.stream()
                        .map(this::mapToWorkflowStatusDto)
                        .collect(Collectors.toList()))
                .workflowTransitions(transitions.stream()
                        .map(this::mapToWorkflowTransitionDto)
                        .collect(Collectors.toList()))
                .issueTypeScheme(issueTypeSchemeInfo)
                .workflowScheme(workflowSchemeInfo)
                .permissionScheme(permissionSchemeInfo)
                .notificationScheme(notificationSchemeInfo)
                .screenScheme(screenSchemeInfo)
                .build();
    }

    /**
     * Get simple template response (without full workflow details)
     */
    public ProjectTemplateResponse getTemplate(UUID templateId) {
        log.debug("Fetching template: {}", templateId);
        ProjectTemplate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));
        return mapToTemplateResponse(template);
    }

    /**
     * Get all templates with basic info
     */
    public List<ProjectTemplateResponse> getAllTemplates() {
        log.debug("Fetching all templates");
        return projectTemplateRepository.findAll().stream()
                .map(this::mapToTemplateResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get available workflow statuses (for creating new templates)
     */
    public List<TemplateWithWorkflowResponse.TemplateWorkflowStatusDto> getAvailableStatuses() {
        log.debug("Fetching available workflow statuses");
        List<StatusDefinition> definitions = statusDefinitionRepository.findAll();

        return definitions.stream()
                .map(d -> TemplateWithWorkflowResponse.TemplateWorkflowStatusDto.builder()
                        .statusName(d.getStatusName())
                        .statusKey(d.getStatusKey())
                        .statusColor(d.getStatusColor())
                        .statusCategory(d.getStatusCategory())
                        .description(d.getDescription())
                        .icon(d.getStatusIcon())
                        .build())
                .collect(Collectors.toList());
    }

    // ============ Helper Methods ============

    private String getCategoryIcon(String category) {
        return switch (category.toUpperCase()) {
            case "BUSINESS" -> "briefcase";
            case "SOFTWARE" -> "code";
            case "TEAM_MANAGED" -> "users";
            default -> "folder";
        };
    }

    private ProjectTemplateResponse mapToTemplateResponse(ProjectTemplate template) {
        ProjectType type = template.getType();
        return ProjectTemplateResponse.builder()
                .id(template.getId())
                .typeId(type.getId())
                .typeName(type.getName())
                .name(template.getName())
                .description(template.getDescription())
                .icon(template.getIcon())
                .color(template.getColor())
                .defaultAssigneeType(template.getDefaultAssigneeType())
                .allowIssueCreation(template.getAllowIssueCreation())
                .sortOrder(template.getSortOrder())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .build();
    }

    private TemplateIssueTypeDto mapToIssueTypeDto(TemplateIssueType issueType) {
        return TemplateIssueTypeDto.builder()
                .id(issueType.getId())
                .issueTypeName(issueType.getIssueTypeName())
                .issueTypeIcon(issueType.getIssueTypeIcon())
                .isDefault(issueType.getIsDefault())
                .isSubtask(issueType.getIsSubtask())
                .sequence(issueType.getSequence())
                .build();
    }

    private TemplateWithWorkflowResponse.TemplateIssueTypeDto mapToWorkflowIssueTypeDto(TemplateIssueType issueType) {
        return TemplateWithWorkflowResponse.TemplateIssueTypeDto.builder()
                .id(issueType.getId())
                .issueTypeName(issueType.getIssueTypeName())
                .issueTypeIcon(issueType.getIssueTypeIcon())
                .isDefault(issueType.getIsDefault())
                .isSubtask(issueType.getIsSubtask())
                .sequence(issueType.getSequence())
                .build();
    }

    private TemplateWithWorkflowResponse.TemplateWorkflowStatusDto mapToWorkflowStatusDto(TemplateWorkflowStatus status) {
        return TemplateWithWorkflowResponse.TemplateWorkflowStatusDto.builder()
                .id(status.getId())
                .statusName(status.getStatusName())
                .statusKey(status.getStatusKey())
                .statusColor(status.getStatusColor())
                .statusCategory(status.getStatusCategory())
                .sequence(status.getSequence())
                .description(status.getDescription())
                .icon(status.getIcon())
                .build();
    }

    private TemplateWithWorkflowResponse.TemplateWorkflowTransitionDto mapToWorkflowTransitionDto(TemplateWorkflowTransition transition) {
        return TemplateWithWorkflowResponse.TemplateWorkflowTransitionDto.builder()
                .id(transition.getId())
                .fromStatusKey(transition.getFromStatusKey())
                .toStatusKey(transition.getToStatusKey())
                .transitionName(transition.getTransitionName())
                .transitionIcon(transition.getTransitionIcon())
                .allowBackward(transition.getAllowBackward())
                .requiresApproval(transition.getRequiresApproval())
                .sequence(transition.getSequence())
                .build();
    }

    private TemplateWithWorkflowResponse.SchemeInfoDto buildSchemeInfo(List<TemplateSchemeMapping> mappings, String schemeType) {
        return mappings.stream()
                .filter(m -> schemeType.equals(m.getSchemeType()))
                .findFirst()
                .map(m -> TemplateWithWorkflowResponse.SchemeInfoDto.builder()
                        .id(m.getSchemeId())
                        .name(m.getSchemeName())
                        .build())
                .orElse(null);
    }
}