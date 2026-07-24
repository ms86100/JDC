package com.jira.project.service;

import com.jira.project.dto.AssignIssueTypeSchemeRequest;
import com.jira.project.dto.AssignWorkflowSchemeRequest;
import com.jira.project.dto.ProjectSchemeResponse;
import com.jira.project.dto.ProjectSchemesBundleResponse;
import com.jira.project.dto.ProjectScreenResolutionResponse;
import com.jira.project.dto.ProjectSchemesExportDto;
import com.jira.project.entity.*;
import com.jira.project.exception.ResourceNotFoundException;
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

    private final ProjectRepository projectRepository;
    private final ProjectSchemeRepository projectSchemeRepository;
    private final IssueTypeSchemeRepository issueTypeSchemeRepository;
    private final IssueTypeSchemeMappingRepository issueTypeSchemeMappingRepository;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final WorkflowSchemeWorkflowRepository workflowSchemeWorkflowRepository;
    private final PermissionSchemeRepository permissionSchemeRepository;
    private final NotificationSchemeRepository notificationSchemeRepository;
    private final ScreenSchemeRepository screenSchemeRepository;
    private final ScreenSchemeScreenRepository screenSchemeScreenRepository;
    private final ScreenSchemeIssueTypeScreenRepository screenSchemeIssueTypeScreenRepository;
    private final FieldConfigurationSchemeRepository fieldConfigurationSchemeRepository;
    private final TemplateSchemeMappingRepository templateSchemeMappingRepository;
    private final TemplateSchemeDefaultRepository templateSchemeDefaultRepository;
    private final ProjectTemplateRepository projectTemplateRepository;

    public ProjectSchemeResponse getSchemeByProjectId(UUID projectId) {
        ProjectScheme scheme = projectSchemeRepository.findByProjectId(projectId)
                .orElse(null);

        if (scheme == null) {
            return null;
        }

        return buildSchemeResponse(scheme);
    }

    public ProjectSchemesBundleResponse getSchemesBundle(UUID projectId) {
        ProjectScheme scheme = projectSchemeRepository.findByProjectId(projectId).orElse(null);
        if (scheme == null) {
            return ProjectSchemesBundleResponse.builder()
                    .projectId(projectId)
                    .build();
        }
        return ProjectSchemesBundleResponse.builder()
                .projectId(projectId)
                .projectSchemeId(scheme.getId())
                .issueTypeSchemeId(scheme.getIssueTypeScheme() != null ? scheme.getIssueTypeScheme().getId() : null)
                .workflowSchemeId(scheme.getWorkflowScheme() != null ? scheme.getWorkflowScheme().getId() : null)
                .permissionSchemeId(scheme.getPermissionScheme() != null ? scheme.getPermissionScheme().getId() : null)
                .notificationSchemeId(scheme.getNotificationScheme() != null ? scheme.getNotificationScheme().getId() : null)
                .screenSchemeId(scheme.getScreenScheme() != null ? scheme.getScreenScheme().getId() : null)
                .fieldConfigurationSchemeId(scheme.getFieldConfigurationScheme() != null
                        ? scheme.getFieldConfigurationScheme().getId() : null)
                .build();
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

        // Assign template-specific schemes from mappings, then template_scheme_defaults
        if (templateId != null) {
            applyTemplateSchemeMappings(templateId, builder);
            applyTemplateSchemeDefaults(templateId, builder);
        }

        // Fall back to default schemes for any not yet assigned
        ProjectScheme partial = builder.build();
        if (partial.getIssueTypeScheme() == null) {
            issueTypeSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::issueTypeScheme);
        }
        partial = builder.build();
        if (partial.getWorkflowScheme() == null) {
            workflowSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::workflowScheme);
        }
        partial = builder.build();
        if (partial.getPermissionScheme() == null) {
            permissionSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::permissionScheme);
        }
        partial = builder.build();
        if (partial.getNotificationScheme() == null) {
            notificationSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::notificationScheme);
        }
        partial = builder.build();
        if (partial.getScreenScheme() == null) {
            screenSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::screenScheme);
        }
        partial = builder.build();
        if (partial.getFieldConfigurationScheme() == null) {
            fieldConfigurationSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(builder::fieldConfigurationScheme);
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

    private void applyTemplateSchemeMappings(UUID templateId, ProjectScheme.ProjectSchemeBuilder builder) {
        List<TemplateSchemeMapping> templateSchemes = templateSchemeMappingRepository.findByTemplateId(templateId);
        log.debug("Found {} template scheme mappings for template: {}", templateSchemes.size(), templateId);

        for (TemplateSchemeMapping mapping : templateSchemes) {
            applySchemeMapping(mapping, builder);
        }
    }

    private void applyTemplateSchemeDefaults(UUID templateId, ProjectScheme.ProjectSchemeBuilder builder) {
        templateSchemeDefaultRepository.findByTemplateId(templateId).ifPresent(defaults -> {
            if (builder.build().getIssueTypeScheme() == null && defaults.getIssueTypeScheme() != null) {
                builder.issueTypeScheme(defaults.getIssueTypeScheme());
            }
            if (builder.build().getWorkflowScheme() == null && defaults.getWorkflowScheme() != null) {
                builder.workflowScheme(defaults.getWorkflowScheme());
            }
            if (builder.build().getPermissionScheme() == null && defaults.getPermissionScheme() != null) {
                builder.permissionScheme(defaults.getPermissionScheme());
            }
            if (builder.build().getNotificationScheme() == null && defaults.getNotificationScheme() != null) {
                builder.notificationScheme(defaults.getNotificationScheme());
            }
            if (builder.build().getScreenScheme() == null && defaults.getScreenScheme() != null) {
                builder.screenScheme(defaults.getScreenScheme());
            }
        });
    }

    private void applySchemeMapping(TemplateSchemeMapping mapping, ProjectScheme.ProjectSchemeBuilder builder) {
        UUID schemeId = mapping.getSchemeId();
        String schemeName = mapping.getSchemeName();

        switch (mapping.getSchemeType()) {
            case TemplateSchemeMapping.SCHEME_TYPE_ISSUE_TYPE:
                if (builder.build().getIssueTypeScheme() == null) {
                    issueTypeSchemeRepository.findById(schemeId)
                            .or(() -> issueTypeSchemeRepository.findByName(schemeName))
                            .ifPresent(builder::issueTypeScheme);
                }
                break;
            case TemplateSchemeMapping.SCHEME_TYPE_WORKFLOW:
                if (builder.build().getWorkflowScheme() == null) {
                    workflowSchemeRepository.findById(schemeId)
                            .or(() -> workflowSchemeRepository.findByName(schemeName))
                            .ifPresent(builder::workflowScheme);
                }
                break;
            case TemplateSchemeMapping.SCHEME_TYPE_PERMISSION:
                if (builder.build().getPermissionScheme() == null) {
                    permissionSchemeRepository.findById(schemeId)
                            .or(() -> permissionSchemeRepository.findByName(schemeName))
                            .ifPresent(builder::permissionScheme);
                }
                break;
            case TemplateSchemeMapping.SCHEME_TYPE_NOTIFICATION:
                if (builder.build().getNotificationScheme() == null) {
                    notificationSchemeRepository.findById(schemeId)
                            .or(() -> notificationSchemeRepository.findByName(schemeName))
                            .ifPresent(builder::notificationScheme);
                }
                break;
            case TemplateSchemeMapping.SCHEME_TYPE_SCREEN:
                if (builder.build().getScreenScheme() == null) {
                    screenSchemeRepository.findById(schemeId)
                            .or(() -> screenSchemeRepository.findByName(schemeName))
                            .ifPresent(builder::screenScheme);
                }
                break;
            default:
                break;
        }
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
            var defaultMappings = workflowSchemeWorkflowRepository
                    .findBySchemeIdAndIssueTypeNameIsNull(workflowScheme.getId());
            String defaultWorkflowName = defaultMappings.isEmpty() ? null : defaultMappings.get(0).getWorkflowName();

            var typeMappings = workflowSchemeWorkflowRepository
                    .findBySchemeIdAndIssueTypeNameIsNotNull(workflowScheme.getId());
            ProjectSchemeResponse.WorkflowMappingInfo[] mappingInfos = typeMappings.stream()
                    .map(m -> ProjectSchemeResponse.WorkflowMappingInfo.builder()
                            .issueTypeName(m.getIssueTypeName())
                            .workflowName(m.getWorkflowName())
                            .build())
                    .toArray(ProjectSchemeResponse.WorkflowMappingInfo[]::new);

            builder.workflowScheme(ProjectSchemeResponse.WorkflowSchemeInfo.builder()
                    .id(workflowScheme.getId())
                    .name(workflowScheme.getName())
                    .defaultWorkflowName(defaultWorkflowName)
                    .mappings(mappingInfos)
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

            var overrides = screenSchemeIssueTypeScreenRepository.findBySchemeId(screenScheme.getId());
            ProjectSchemeResponse.IssueTypeScreenOverrideInfo[] overrideInfos = overrides.stream()
                    .map(o -> ProjectSchemeResponse.IssueTypeScreenOverrideInfo.builder()
                            .issueTypeId(o.getIssueTypeId())
                            .screenType(o.getScreenType())
                            .screenId(o.getScreenId())
                            .build())
                    .toArray(ProjectSchemeResponse.IssueTypeScreenOverrideInfo[]::new);

            builder.screenScheme(ProjectSchemeResponse.ScreenSchemeInfo.builder()
                    .id(screenScheme.getId())
                    .name(screenScheme.getName())
                    .screens(screenMappings)
                    .issueTypeOverrides(overrideInfos)
                    .build());
        }

        if (scheme.getFieldConfigurationScheme() != null) {
            FieldConfigurationScheme fieldScheme = scheme.getFieldConfigurationScheme();
            builder.fieldConfigurationScheme(ProjectSchemeResponse.FieldConfigurationSchemeInfo.builder()
                    .id(fieldScheme.getId())
                    .name(fieldScheme.getName())
                    .build());
        }

        return builder.build();
    }

    public ProjectScheme getSchemeEntityByProjectId(UUID projectId) {
        return projectSchemeRepository.findByProjectId(projectId).orElse(null);
    }

    @Transactional
    public ProjectScheme linkSchemesToProject(UUID projectId, ProjectSchemesExportDto schemes) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        ProjectScheme scheme = projectSchemeRepository.findByProjectId(projectId).orElse(null);
        if (scheme == null) {
            scheme = ProjectScheme.builder().project(project).build();
        }

        if (schemes.getIssueTypeSchemeId() != null) {
            issueTypeSchemeRepository.findById(schemes.getIssueTypeSchemeId())
                    .ifPresent(scheme::setIssueTypeScheme);
        }
        if (schemes.getWorkflowSchemeId() != null) {
            workflowSchemeRepository.findById(schemes.getWorkflowSchemeId())
                    .ifPresent(scheme::setWorkflowScheme);
        }
        if (schemes.getPermissionSchemeId() != null) {
            permissionSchemeRepository.findById(schemes.getPermissionSchemeId())
                    .ifPresent(scheme::setPermissionScheme);
        }
        if (schemes.getNotificationSchemeId() != null) {
            notificationSchemeRepository.findById(schemes.getNotificationSchemeId())
                    .ifPresent(scheme::setNotificationScheme);
        }
        if (schemes.getScreenSchemeId() != null) {
            screenSchemeRepository.findById(schemes.getScreenSchemeId())
                    .ifPresent(scheme::setScreenScheme);
        }

        return projectSchemeRepository.save(scheme);
    }

    /**
     * Resolves CREATE/EDIT/VIEW screen IDs for a project. Issue-type-specific overrides
     * can be added when issue-type screen scheme mappings are stored on the project.
     */
    public ProjectScreenResolutionResponse resolveScreens(UUID projectId, UUID issueTypeId) {
        ProjectSchemeResponse scheme = getSchemeByProjectId(projectId);
        UUID createId = null;
        UUID editId = null;
        UUID viewId = null;
        UUID defaultId = null;

        if (scheme != null && scheme.getScreenScheme() != null) {
            UUID screenSchemeId = scheme.getScreenScheme().getId();

            if (issueTypeId != null && screenSchemeId != null) {
                for (ScreenSchemeIssueTypeScreen override :
                        screenSchemeIssueTypeScreenRepository.findBySchemeIdAndIssueTypeId(screenSchemeId, issueTypeId)) {
                    if (override.getScreenType() == null || override.getScreenId() == null) {
                        continue;
                    }
                    String type = override.getScreenType().toUpperCase(Locale.ROOT);
                    switch (type) {
                        case "CREATE" -> createId = override.getScreenId();
                        case "EDIT" -> editId = override.getScreenId();
                        case "VIEW" -> viewId = override.getScreenId();
                        case "DEFAULT" -> defaultId = override.getScreenId();
                        default -> { }
                    }
                }
            }

            if (scheme.getScreenScheme().getScreens() != null) {
                for (ProjectSchemeResponse.ScreenMappingInfo mapping : scheme.getScreenScheme().getScreens()) {
                    if (mapping.getScreenType() == null || mapping.getScreenId() == null) {
                        continue;
                    }
                    String type = mapping.getScreenType().toUpperCase(Locale.ROOT);
                    switch (type) {
                        case "CREATE" -> {
                            if (createId == null) createId = mapping.getScreenId();
                        }
                        case "EDIT" -> {
                            if (editId == null) editId = mapping.getScreenId();
                        }
                        case "VIEW" -> {
                            if (viewId == null) viewId = mapping.getScreenId();
                        }
                        case "DEFAULT" -> {
                            if (defaultId == null) defaultId = mapping.getScreenId();
                        }
                        default -> { }
                    }
                }
            }
        }

        if (createId == null) {
            createId = defaultId;
        }
        if (editId == null) {
            editId = defaultId;
        }
        if (viewId == null) {
            viewId = defaultId;
        }

        return ProjectScreenResolutionResponse.builder()
                .projectId(projectId)
                .issueTypeId(issueTypeId)
                .createScreenId(createId)
                .editScreenId(editId)
                .viewScreenId(viewId)
                .defaultScreenId(defaultId)
                .build();
    }
}