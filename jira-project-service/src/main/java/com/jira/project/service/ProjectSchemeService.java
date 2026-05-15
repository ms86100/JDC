package com.jira.project.service;

import com.jira.project.dto.ProjectSchemeResponse;
import com.jira.project.entity.*;
import com.jira.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

        // Assign default schemes
        issueTypeSchemeRepository.findByIsDefaultTrue()
                .ifPresent(builder::issueTypeScheme);
        workflowSchemeRepository.findByIsDefaultTrue()
                .ifPresent(builder::workflowScheme);
        permissionSchemeRepository.findByIsDefaultTrue()
                .ifPresent(builder::permissionScheme);
        notificationSchemeRepository.findByIsDefaultTrue()
                .ifPresent(builder::notificationScheme);
        screenSchemeRepository.findByIsDefaultTrue()
                .ifPresent(builder::screenScheme);

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