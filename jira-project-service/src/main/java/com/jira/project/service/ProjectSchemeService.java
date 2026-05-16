package com.jira.project.service;

import com.jira.project.dto.ProjectSchemeResponse;
import com.jira.project.entity.*;
import com.jira.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectSchemeService {

    private final ProjectSchemeRepository projectSchemeRepository;
    private final IssueTypeSchemeRepository issueTypeSchemeRepository;
    private final IssueTypeSchemeMappingRepository issueTypeSchemeMappingRepository;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final WorkflowSchemeWorkflowRepository workflowSchemeWorkflowRepository;
    private final PermissionSchemeRepository permissionSchemeRepository;
    private final NotificationSchemeRepository notificationSchemeRepository;
    private final ScreenSchemeRepository screenSchemeRepository;
    private final ScreenSchemeScreenRepository screenSchemeScreenRepository;
    private final TemplateSchemeMappingRepository templateSchemeMappingRepository;
    private final ProjectTemplateRepository projectTemplateRepository;

    public ProjectSchemeResponse getSchemeByProjectId(UUID projectId) {
        ProjectScheme scheme = projectSchemeRepository.findByProjectId(projectId)
                .orElse(null);

        if (scheme == null) {
            return null;
        }

        return buildSchemeResponse(scheme);
    }

    @Transactional
    public ProjectScheme createProjectScheme(Project project, UUID templateId) {
        ProjectScheme.ProjectSchemeBuilder builder = ProjectScheme.builder()
                .project(project);

        // First, try to assign template-specific schemes if templateId is provided
        if (templateId != null) {
            List<TemplateSchemeMapping> templateSchemes = templateSchemeMappingRepository.findByTemplateId(templateId);

            if (!templateSchemes.isEmpty()) {
                log.debug("Assigning template-specific schemes for template: {}", templateId);

                for (TemplateSchemeMapping mapping : templateSchemes) {
                    UUID schemeId = mapping.getSchemeId();
                    String schemeName = mapping.getSchemeName();

                    switch (mapping.getSchemeType()) {
                        case TemplateSchemeMapping.SCHEME_TYPE_ISSUE_TYPE:
                            issueTypeSchemeRepository.findById(schemeId)
                                    .or(() -> issueTypeSchemeRepository.findByName(schemeName))
                                    .ifPresent(builder::issueTypeScheme);
                            break;
                        case TemplateSchemeMapping.SCHEME_TYPE_WORKFLOW:
                            workflowSchemeRepository.findById(schemeId)
                                    .or(() -> workflowSchemeRepository.findByName(schemeName))
                                    .ifPresent(builder::workflowScheme);
                            break;
                        case TemplateSchemeMapping.SCHEME_TYPE_PERMISSION:
                            permissionSchemeRepository.findById(schemeId)
                                    .or(() -> permissionSchemeRepository.findByName(schemeName))
                                    .ifPresent(builder::permissionScheme);
                            break;
                        case TemplateSchemeMapping.SCHEME_TYPE_NOTIFICATION:
                            notificationSchemeRepository.findById(schemeId)
                                    .or(() -> notificationSchemeRepository.findByName(schemeName))
                                    .ifPresent(builder::notificationScheme);
                            break;
                        case TemplateSchemeMapping.SCHEME_TYPE_SCREEN:
                            screenSchemeRepository.findById(schemeId)
                                    .or(() -> screenSchemeRepository.findByName(schemeName))
                                    .ifPresent(builder::screenScheme);
                            break;
                    }
                }
            }
        }

        // Fall back to default schemes for any not yet assigned
        if (builder.build().getIssueTypeScheme() == null) {
            issueTypeSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::issueTypeScheme);
        }
        if (builder.build().getWorkflowScheme() == null) {
            workflowSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::workflowScheme);
        }
        if (builder.build().getPermissionScheme() == null) {
            permissionSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::permissionScheme);
        }
        if (builder.build().getNotificationScheme() == null) {
            notificationSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::notificationScheme);
        }
        if (builder.build().getScreenScheme() == null) {
            screenSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::screenScheme);
        }

        ProjectScheme projectScheme = builder.build();
        return projectSchemeRepository.save(projectScheme);
    }

    private ProjectSchemeResponse buildSchemeResponse(ProjectScheme scheme) {
        ProjectSchemeResponse.ProjectSchemeResponseBuilder builder = ProjectSchemeResponse.builder()
                .id(scheme.getId())
                .projectId(scheme.getProject().getId());

        if (scheme.getIssueTypeScheme() != null) {
            IssueTypeScheme issueTypeScheme = scheme.getIssueTypeScheme();
            var mappings = issueTypeSchemeMappingRepository.findBySchemeId(issueTypeScheme.getId());

            String[] issueTypeNames = mappings.stream()
                    .map(IssueTypeSchemeMapping::getIssueTypeName)
                    .toArray(String[]::new);

            String defaultIssueTypeName = mappings.stream()
                    .filter(IssueTypeSchemeMapping::getIsDefault)
                    .map(IssueTypeSchemeMapping::getIssueTypeName)
                    .findFirst()
                    .orElse(null);

            builder.issueTypeScheme(ProjectSchemeResponse.IssueTypeSchemeInfo.builder()
                    .id(issueTypeScheme.getId())
                    .name(issueTypeScheme.getName())
                    .issueTypeNames(issueTypeNames)
                    .defaultIssueTypeName(defaultIssueTypeName)
                    .build());
        }

        if (scheme.getWorkflowScheme() != null) {
            WorkflowScheme workflowScheme = scheme.getWorkflowScheme();
            var mappings = workflowSchemeWorkflowRepository.findBySchemeIdAndIssueTypeNameIsNull(workflowScheme.getId());
            String defaultWorkflowName = mappings.isEmpty() ? null : mappings.get(0).getWorkflowName();

            builder.workflowScheme(ProjectSchemeResponse.WorkflowSchemeInfo.builder()
                    .id(workflowScheme.getId())
                    .name(workflowScheme.getName())
                    .defaultWorkflowName(defaultWorkflowName)
                    .build());
        }

        if (scheme.getPermissionScheme() != null) {
            PermissionScheme permissionScheme = scheme.getPermissionScheme();
            builder.permissionScheme(ProjectSchemeResponse.PermissionSchemeInfo.builder()
                    .id(permissionScheme.getId())
                    .name(permissionScheme.getName())
                    .permissions(permissionScheme.getPermissions())
                    .build());
        }

        if (scheme.getNotificationScheme() != null) {
            NotificationScheme notificationScheme = scheme.getNotificationScheme();
            builder.notificationScheme(ProjectSchemeResponse.NotificationSchemeInfo.builder()
                    .id(notificationScheme.getId())
                    .name(notificationScheme.getName())
                    .notifications(notificationScheme.getNotifications())
                    .build());
        }

        if (scheme.getScreenScheme() != null) {
            ScreenScheme screenScheme = scheme.getScreenScheme();
            var screens = screenSchemeScreenRepository.findBySchemeId(screenScheme.getId());

            ProjectSchemeResponse.ScreenMappingInfo[] screenMappings = screens.stream()
                    .map(s -> ProjectSchemeResponse.ScreenMappingInfo.builder()
                            .screenType(s.getScreenType())
                            .screenId(s.getScreenId())
                            .build())
                    .toArray(ProjectSchemeResponse.ScreenMappingInfo[]::new);

            builder.screenScheme(ProjectSchemeResponse.ScreenSchemeInfo.builder()
                    .id(screenScheme.getId())
                    .name(screenScheme.getName())
                    .screens(screenMappings)
                    .build());
        }

        return builder.build();
    }
}