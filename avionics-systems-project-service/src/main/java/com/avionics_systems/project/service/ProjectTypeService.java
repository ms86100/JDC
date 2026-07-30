package com.avionics_systems.project.service;

import com.avionics_systems.project.dto.ProjectTemplateResponse;
import com.avionics_systems.project.dto.ProjectTypeResponse;
import com.avionics_systems.project.dto.TemplateDetailsResponse;
import com.avionics_systems.project.entity.*;
import com.avionics_systems.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectTypeService {

    private final ProjectTypeRepository projectTypeRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final TemplateSchemeDefaultRepository templateSchemeDefaultRepository;
    private final IssueTypeSchemeRepository issueTypeSchemeRepository;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final PermissionSchemeRepository permissionSchemeRepository;
    private final NotificationSchemeRepository notificationSchemeRepository;
    private final ScreenSchemeRepository screenSchemeRepository;

    @Value("${app.defaults.role-names:PROJECT_ADMIN,DEVELOPER,VIEWER}")
    private String defaultRoleNamesStr;

    public List<ProjectTypeResponse> getAllActiveTypes() {
        return projectTypeRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::mapToTypeResponse)
                .collect(Collectors.toList());
    }

    public ProjectTypeResponse getTypeById(UUID typeId) {
        return projectTypeRepository.findById(typeId)
                .map(this::mapToTypeResponse)
                .orElseThrow(() -> new RuntimeException("Project type not found: " + typeId));
    }

    public List<ProjectTemplateResponse> getTemplatesForType(UUID typeId) {
        return projectTemplateRepository.findActiveTemplatesByTypeId(typeId)
                .stream()
                .map(this::mapToTemplateResponse)
                .collect(Collectors.toList());
    }

    public ProjectTemplateResponse getTemplateById(UUID templateId) {
        return projectTemplateRepository.findById(templateId)
                .map(this::mapToTemplateResponse)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
    }

    public TemplateDetailsResponse getTemplateDetails(UUID templateId) {
        ProjectTemplate template = projectTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        TemplateSchemeDefault schemeDefaults = templateSchemeDefaultRepository.findByTemplateId(templateId)
                .orElse(null);

        return TemplateDetailsResponse.builder()
                .templateId(template.getId())
                .templateName(template.getName())
                .icon(template.getIcon())
                .color(template.getColor())
                .defaultAssigneeType(template.getDefaultAssigneeType())
                .allowIssueCreation(template.getAllowIssueCreation())
                .issueTypeSchemeId(schemeDefaults != null && schemeDefaults.getIssueTypeScheme() != null
                        ? schemeDefaults.getIssueTypeScheme().getId() : null)
                .issueTypeSchemeName(schemeDefaults != null && schemeDefaults.getIssueTypeScheme() != null
                        ? schemeDefaults.getIssueTypeScheme().getName() : null)
                .workflowSchemeId(schemeDefaults != null && schemeDefaults.getWorkflowScheme() != null
                        ? schemeDefaults.getWorkflowScheme().getId() : null)
                .workflowSchemeName(schemeDefaults != null && schemeDefaults.getWorkflowScheme() != null
                        ? schemeDefaults.getWorkflowScheme().getName() : null)
                .permissionSchemeId(schemeDefaults != null && schemeDefaults.getPermissionScheme() != null
                        ? schemeDefaults.getPermissionScheme().getId() : null)
                .permissionSchemeName(schemeDefaults != null && schemeDefaults.getPermissionScheme() != null
                        ? schemeDefaults.getPermissionScheme().getName() : null)
                .notificationSchemeId(schemeDefaults != null && schemeDefaults.getNotificationScheme() != null
                        ? schemeDefaults.getNotificationScheme().getId() : null)
                .notificationSchemeName(schemeDefaults != null && schemeDefaults.getNotificationScheme() != null
                        ? schemeDefaults.getNotificationScheme().getName() : null)
                .screenSchemeId(schemeDefaults != null && schemeDefaults.getScreenScheme() != null
                        ? schemeDefaults.getScreenScheme().getId() : null)
                .screenSchemeName(schemeDefaults != null && schemeDefaults.getScreenScheme() != null
                        ? schemeDefaults.getScreenScheme().getName() : null)
                .defaultRoles(defaultRoleNamesStr.split(","))
                .build();
    }

    private ProjectTypeResponse mapToTypeResponse(ProjectType type) {
        return ProjectTypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .description(type.getDescription())
                .category(type.getCategory())
                .icon(type.getIcon())
                .sortOrder(type.getSortOrder())
                .isActive(type.getIsActive())
                .createdAt(type.getCreatedAt())
                .build();
    }

    private ProjectTemplateResponse mapToTemplateResponse(ProjectTemplate template) {
        return ProjectTemplateResponse.builder()
                .id(template.getId())
                .typeId(template.getType().getId())
                .typeName(template.getType().getName())
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
}