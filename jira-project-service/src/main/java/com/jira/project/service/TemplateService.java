package com.jira.project.service;

import com.jira.project.dto.*;
import com.jira.project.entity.*;
import com.jira.project.exception.ResourceNotFoundException;
import com.jira.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final TemplateCategoryRepository templateCategoryRepository;
    private final TemplateCapabilityRepository templateCapabilityRepository;
    private final TemplateWorkflowStatusRepository templateWorkflowStatusRepository;
    private final TemplateWorkflowTransitionRepository templateWorkflowTransitionRepository;
    private final TemplateIssueTypeRepository templateIssueTypeRepository;
    private final TemplateSchemeMappingRepository templateSchemeMappingRepository;
    private final StatusDefinitionRepository statusDefinitionRepository;

    /**
     * Full catalog for Create Project wizard (Jira DC-style two-panel UI).
     */
    public TemplateCatalogResponse getTemplateCatalog() {
        log.debug("Fetching template catalog");

        List<ProjectTemplate> activeTemplates = projectTemplateRepository.findAll().stream()
                .filter(ProjectTemplate::getIsActive)
                .sorted(Comparator.comparing(
                        ProjectTemplate::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        Map<UUID, List<TemplateCapabilityDto>> capabilitiesByTemplate = loadCapabilities(activeTemplates);

        List<TemplateCategory> categories = templateCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<TemplateCatalogResponse.TemplateCategoryCatalogDto> categoryDtos = new ArrayList<>();

        for (TemplateCategory category : categories) {
            List<ProjectTemplateResponse> templates = activeTemplates.stream()
                    .filter(t -> t.getTemplateCategory() != null
                            && category.getId().equals(t.getTemplateCategory().getId()))
                    .map(t -> mapToTemplateResponse(t, capabilitiesByTemplate.get(t.getId())))
                    .collect(Collectors.toList());

            if (!templates.isEmpty()) {
                categoryDtos.add(TemplateCatalogResponse.TemplateCategoryCatalogDto.builder()
                        .categoryKey(category.getCategoryKey())
                        .name(category.getName())
                        .description(category.getDescription())
                        .icon(category.getIcon())
                        .iconEmoji(category.getIconEmoji())
                        .sortOrder(category.getSortOrder())
                        .templates(templates)
                        .build());
            }
        }

        // Uncategorized fallback (legacy templates without category_id)
        List<ProjectTemplateResponse> uncategorized = activeTemplates.stream()
                .filter(t -> t.getTemplateCategory() == null)
                .map(t -> mapToTemplateResponse(t, capabilitiesByTemplate.get(t.getId())))
                .collect(Collectors.toList());

        if (!uncategorized.isEmpty()) {
            categoryDtos.add(TemplateCatalogResponse.TemplateCategoryCatalogDto.builder()
                    .categoryKey("OTHER")
                    .name("Other")
                    .description("Additional project templates")
                    .icon("folder")
                    .iconEmoji("📁")
                    .sortOrder(99)
                    .templates(uncategorized)
                    .build());
        }

        List<ProjectTemplateResponse> recommended = activeTemplates.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsRecommended()))
                .map(t -> mapToTemplateResponse(t, capabilitiesByTemplate.get(t.getId())))
                .collect(Collectors.toList());

        return TemplateCatalogResponse.builder()
                .categories(categoryDtos)
                .recommended(recommended)
                .recentlyUsed(List.of())
                .build();
    }

    /**
     * Get all active templates grouped by category
     */
    public List<TemplateCategoryResponse> getTemplatesByCategory() {
        log.debug("Fetching templates grouped by category");

        TemplateCatalogResponse catalog = getTemplateCatalog();

        return catalog.getCategories().stream()
                .map(cat -> TemplateCategoryResponse.builder()
                        .categoryKey(cat.getCategoryKey())
                        .categoryName(cat.getName())
                        .categoryDescription(cat.getDescription())
                        .categoryIcon(cat.getIcon())
                        .categoryIconEmoji(cat.getIconEmoji())
                        .sortOrder(cat.getSortOrder())
                        .templates(cat.getTemplates())
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

    private Map<UUID, List<TemplateCapabilityDto>> loadCapabilities(List<ProjectTemplate> templates) {
        if (templates.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = templates.stream().map(ProjectTemplate::getId).collect(Collectors.toList());
        return templateCapabilityRepository.findByTemplateIdInOrderBySortOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        TemplateCapability::getTemplateId,
                        Collectors.mapping(c -> TemplateCapabilityDto.builder()
                                .key(c.getCapabilityKey())
                                .label(c.getCapabilityLabel())
                                .group(c.getCapabilityGroup())
                                .build(), Collectors.toList())
                ));
    }

    private ProjectTemplateResponse mapToTemplateResponse(ProjectTemplate template) {
        return mapToTemplateResponse(template, List.of());
    }

    private ProjectTemplateResponse mapToTemplateResponse(ProjectTemplate template, List<TemplateCapabilityDto> capabilities) {
        ProjectType type = template.getType();
        TemplateCategory cat = template.getTemplateCategory();
        if (type == null) {
            throw new IllegalStateException(
                    "Template " + template.getId() + " (" + template.getName() + ") has no project type");
        }

        return ProjectTemplateResponse.builder()
                .id(template.getId())
                .typeId(type.getId())
                .typeName(type.getName())
                .name(template.getName())
                .description(template.getDescription())
                .icon(template.getIcon())
                .color(template.getColor() != null ? template.getColor() : template.getPreviewAccent())
                .defaultAssigneeType(template.getDefaultAssigneeType())
                .allowIssueCreation(template.getAllowIssueCreation())
                .sortOrder(template.getSortOrder())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .categoryKey(cat != null ? cat.getCategoryKey() : template.getCategory())
                .categoryName(cat != null ? cat.getName() : null)
                .templateType(template.getTemplateType())
                .workflowType(template.getWorkflowType())
                .workflowTypeLabel(formatWorkflowTypeLabel(template.getWorkflowType()))
                .shortDescription(template.getShortDescription())
                .iconEmoji(template.getIconEmoji())
                .useCases(template.getUseCases())
                .instructions(template.getInstructions())
                .previewAccent(template.getPreviewAccent() != null ? template.getPreviewAccent() : template.getColor())
                .recommended(template.getIsRecommended())
                .projectTypeCategory(type.getCategory())
                .capabilities(capabilities != null ? capabilities : List.of())
                .build();
    }

    private String formatWorkflowTypeLabel(String workflowType) {
        if (workflowType == null) {
            return "General";
        }
        return switch (workflowType) {
            case "AGILE_SCRUM" -> "Agile · Scrum";
            case "AGILE_KANBAN" -> "Agile · Kanban";
            case "DEFECT_TRACKING" -> "Defect Tracking";
            case "TASK" -> "Task-based";
            case "PORTFOLIO" -> "Portfolio";
            case "PROCESS" -> "Process-based";
            case "TEAM_MANAGED" -> "Team-managed";
            default -> workflowType.replace('_', ' ');
        };
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