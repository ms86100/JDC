package com.jira.project.service;

import com.jira.project.dto.AssignIssueTypeSchemeRequest;
import com.jira.project.dto.AssignWorkflowSchemeRequest;
import com.jira.project.dto.ProjectSchemeResponse;
import com.jira.project.entity.*;
import com.jira.project.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
        log.info("Creating project scheme for project: {} with template: {}", project.getId(), templateId);

        // Check if scheme already exists for this project (idempotency)
        if (projectSchemeRepository.existsByProjectId(project.getId())) {
            log.debug("Project scheme already exists for project: {}, returning existing", project.getId());
            return projectSchemeRepository.findByProjectId(project.getId()).orElse(null);
        }

        log.debug("No existing scheme found, creating new one for project: {}", project.getId());
        ProjectScheme.ProjectSchemeBuilder builder = ProjectScheme.builder()
                .project(project);

        // First, try to assign template-specific schemes if templateId is provided
        if (templateId != null) {
            List<TemplateSchemeMapping> templateSchemes = templateSchemeMappingRepository.findByTemplateId(templateId);
            log.debug("Found {} template scheme mappings for template: {}", templateSchemes.size(), templateId);

            if (!templateSchemes.isEmpty()) {
                log.debug("Assigning template-specific schemes for template: {}", templateId);

                for (TemplateSchemeMapping mapping : templateSchemes) {
                    UUID schemeId = mapping.getSchemeId();
                    String schemeName = mapping.getSchemeName();

                    log.debug("Processing scheme mapping - type: {}, name: {}, id: {}", mapping.getSchemeType(), schemeName, schemeId);

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

    @Transactional
    public int assignIssueTypeSchemeFromAdmin(AssignIssueTypeSchemeRequest request) {
        if (request.getSchemeName() == null || request.getSchemeName().isBlank()) {
            throw new IllegalArgumentException("schemeName is required");
        }

        IssueTypeScheme scheme = issueTypeSchemeRepository.findByName(request.getSchemeName())
                .orElseGet(() -> issueTypeSchemeRepository.save(IssueTypeScheme.builder()
                        .name(request.getSchemeName())
                        .description(request.getDescription())
                        .isDefault(false)
                        .build()));

        if (request.getDescription() != null) {
            scheme.setDescription(request.getDescription());
            issueTypeSchemeRepository.save(scheme);
        }

        UUID schemeId = scheme.getId();
        List<IssueTypeSchemeMapping> existingMappings = issueTypeSchemeMappingRepository.findBySchemeId(schemeId);
        issueTypeSchemeMappingRepository.deleteAll(existingMappings);

        List<String> keys = request.getIssueTypeKeys() != null ? request.getIssueTypeKeys() : List.of();
        String defaultKey = request.getDefaultIssueTypeKey();
        if (defaultKey == null || defaultKey.isBlank()) {
            defaultKey = keys.isEmpty() ? null : keys.get(0);
        }

        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            issueTypeSchemeMappingRepository.save(IssueTypeSchemeMapping.builder()
                    .schemeId(schemeId)
                    .issueTypeName(key)
                    .isDefault(key.equals(defaultKey))
                    .build());
        }

        Set<UUID> selected = new HashSet<>();
        if (request.getProjectIds() != null) {
            for (String id : request.getProjectIds()) {
                try {
                    selected.add(UUID.fromString(id));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Skipping invalid project id in scheme assign: {}", id);
                }
            }
        }

        IssueTypeScheme fallback = issueTypeSchemeRepository.findByIsDefaultTrue().orElse(scheme);
        int updated = 0;

        for (ProjectScheme projectScheme : projectSchemeRepository.findAll()) {
            UUID projectId = projectScheme.getProject().getId();
            IssueTypeScheme current = projectScheme.getIssueTypeScheme();

            if (selected.contains(projectId)) {
                projectScheme.setIssueTypeScheme(scheme);
                projectSchemeRepository.save(projectScheme);
                updated++;
            } else if (current != null && request.getSchemeName().equals(current.getName())) {
                projectScheme.setIssueTypeScheme(fallback);
                projectSchemeRepository.save(projectScheme);
                updated++;
            }
        }

        log.info("Assigned issue type scheme '{}' to {} project(s)", scheme.getName(), selected.size());
        return updated;
    }

    @Transactional
    public int assignWorkflowSchemeFromBridge(AssignWorkflowSchemeRequest request) {
        if (request.getSchemeName() == null || request.getSchemeName().isBlank()) {
            throw new IllegalArgumentException("schemeName is required");
        }

        WorkflowScheme scheme = null;
        if (request.getSchemeId() != null) {
            scheme = workflowSchemeRepository.findById(request.getSchemeId()).orElse(null);
        }
        if (scheme == null) {
            scheme = workflowSchemeRepository.findByName(request.getSchemeName()).orElse(null);
        }
        if (scheme == null) {
            scheme = workflowSchemeRepository.save(WorkflowScheme.builder()
                    .name(request.getSchemeName())
                    .description(request.getDescription())
                    .isDefault(false)
                    .build());
        } else if (request.getDescription() != null) {
            scheme.setDescription(request.getDescription());
            workflowSchemeRepository.save(scheme);
        }

        Set<UUID> selected = new HashSet<>();
        if (request.getProjectIds() != null) {
            for (String id : request.getProjectIds()) {
                try {
                    selected.add(UUID.fromString(id));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Skipping invalid project id in workflow scheme assign: {}", id);
                }
            }
        }

        WorkflowScheme fallback = workflowSchemeRepository.findByIsDefaultTrue().orElse(scheme);
        final WorkflowScheme activeScheme = scheme;
        int updated = 0;

        for (ProjectScheme projectScheme : projectSchemeRepository.findAll()) {
            UUID projectId = projectScheme.getProject().getId();
            WorkflowScheme current = projectScheme.getWorkflowScheme();

            if (selected.contains(projectId)) {
                projectScheme.setWorkflowScheme(activeScheme);
                projectSchemeRepository.save(projectScheme);
                updated++;
            } else if (current != null && request.getSchemeName().equals(current.getName())) {
                projectScheme.setWorkflowScheme(fallback);
                projectSchemeRepository.save(projectScheme);
                updated++;
            }
        }

        log.info("Assigned workflow scheme '{}' to {} project(s) in project-service", activeScheme.getName(), selected.size());
        return updated;
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