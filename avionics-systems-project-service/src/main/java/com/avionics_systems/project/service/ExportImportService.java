package com.avionics_systems.project.service;

import com.avionics_systems.project.dto.*;
import com.avionics_systems.project.entity.Project;
import com.avionics_systems.project.entity.ProjectMember;
import com.avionics_systems.project.entity.ProjectRole;
import com.avionics_systems.project.entity.ProjectScheme;
import com.avionics_systems.project.exception.DuplicateResourceException;
import com.avionics_systems.project.exception.InvalidOperationException;
import com.avionics_systems.project.exception.ResourceNotFoundException;
import com.avionics_systems.project.repository.ProjectMemberRepository;
import com.avionics_systems.project.repository.ProjectRepository;
import com.avionics_systems.project.repository.ProjectRoleRepository;
import com.avionics_systems.project.repository.ProjectSchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportImportService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final ProjectSchemeRepository projectSchemeRepository;
    private final ProjectSchemeService projectSchemeService;

    @Value("${app.defaults.assignee-type:PROJECT_LEAD}")
    private String defaultAssigneeType;

    @Value("${app.defaults.project-type:COMPANY_MANAGED}")
    private String defaultProjectType;

    @Value("${app.defaults.role-names:PROJECT_ADMIN,DEVELOPER,VIEWER}")
    private String defaultRoleNamesStr;

    @Value("${app.defaults.role-admin-name:PROJECT_ADMIN}")
    private String defaultAdminRoleName;

    @Value("${app.defaults.role-admin-description:Project Administrator with full access}")
    private String defaultAdminRoleDescription;

    @Value("${app.defaults.role-developer-description:Developer with edit and create permissions}")
    private String defaultDeveloperRoleDescription;

    @Value("${app.defaults.role-viewer-description:Read-only access}")
    private String defaultViewerRoleDescription;

    @Value("${app.defaults.permissions.admin:*}")
    private String defaultAdminPermissions;

    @Value("${app.defaults.permissions.developer:read,edit,create,comment,transition}")
    private String defaultDeveloperPermissions;

    @Value("${app.defaults.permissions.viewer:read,comment}")
    private String defaultViewerPermissions;

    @Transactional(readOnly = true)
    public ProjectExportDto exportProject(UUID projectId) {
        log.info("Exporting project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        List<ProjectRole> roles = projectRoleRepository.findByProjectId(projectId);

        ProjectScheme scheme = projectSchemeService.getSchemeEntityByProjectId(projectId);

        ProjectSchemesExportDto schemesExport = null;
        if (scheme != null) {
            schemesExport = ProjectSchemesExportDto.builder()
                    .issueTypeSchemeId(getEntityId(scheme.getIssueTypeScheme()))
                    .workflowSchemeId(getEntityId(scheme.getWorkflowScheme()))
                    .permissionSchemeId(getEntityId(scheme.getPermissionScheme()))
                    .notificationSchemeId(getEntityId(scheme.getNotificationScheme()))
                    .screenSchemeId(getEntityId(scheme.getScreenScheme()))
                    .build();
        }

        return ProjectExportDto.builder()
                .projectId(project.getId())
                .projectKey(project.getProjectKey())
                .name(project.getName())
                .description(project.getDescription())
                .leadUserId(project.getLeadUserId())
                .projectType(project.getProjectType())
                .templateId(project.getTemplateId())
                .category(project.getCategory())
                .avatarUrl(project.getAvatarUrl())
                .defaultAssigneeType(project.getDefaultAssigneeType())
                .allowIssueCreation(project.getAllowIssueCreation())
                .exportedAt(LocalDateTime.now())
                .members(members.stream().map(m -> {
                    ProjectRole role = projectRoleRepository.findById(m.getProjectRoleId()).orElse(null);
                    return ProjectMemberExportDto.builder()
                            .userId(m.getUserId())
                            .roleName(role != null ? role.getName() : null)
                            .build();
                }).collect(Collectors.toList()))
                .roles(roles.stream().map(r -> ProjectRoleExportDto.builder()
                        .name(r.getName())
                        .description(r.getDescription())
                        .permissions(r.getPermissions())
                        .build()).collect(Collectors.toList()))
                .schemes(schemesExport)
                .metadata(Map.of(
                        "archived", project.getArchived() != null && project.getArchived(),
                        "version", project.getVersion() != null ? project.getVersion() : 0
                ))
                .build();
    }

    @Transactional
    public ProjectResponse importProject(ProjectImportRequest request) {
        log.info("Importing project: {} with key: {}", request.getName(), request.getProjectKey());

        // Validate key uniqueness
        Optional<Project> existing = projectRepository.findByProjectKey(request.getProjectKey());
        if (existing.isPresent()) {
            throw new DuplicateResourceException("Project with key '" + request.getProjectKey() + "' already exists");
        }

        // Create project
        Project project = Project.builder()
                .projectKey(request.getProjectKey())
                .name(request.getName())
                .description(request.getDescription())
                .leadUserId(request.getLeadUserId())
                .projectType(request.getProjectType() != null ? request.getProjectType() : defaultProjectType)
                .templateId(request.getTemplateId())
                .category(request.getCategory())
                .avatarUrl(request.getAvatarUrl())
                .defaultAssigneeType(request.getDefaultAssigneeType() != null ? request.getDefaultAssigneeType() : defaultAssigneeType)
                .allowIssueCreation(request.getAllowIssueCreation() != null ? request.getAllowIssueCreation() : true)
                .archived(false)
                .build();

        project = projectRepository.save(project);

        // Create roles if provided
        Map<String, UUID> roleIdMap = new HashMap<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (ProjectRoleExportDto roleDto : request.getRoles()) {
                ProjectRole role = ProjectRole.builder()
                        .projectId(project.getId())
                        .name(roleDto.getName())
                        .description(roleDto.getDescription())
                        .permissions(roleDto.getPermissions())
                        .build();
                role = projectRoleRepository.save(role);
                roleIdMap.put(role.getName(), role.getId());
            }
        }

        // Create default roles if none provided
        if (roleIdMap.isEmpty()) {
            createDefaultRoles(project.getId(), roleIdMap);
        }

        // Add members if provided
        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            for (ProjectMemberExportDto memberDto : request.getMembers()) {
                UUID roleId = roleIdMap.get(memberDto.getRoleName());
                if (roleId != null) {
                    ProjectMember member = ProjectMember.builder()
                            .projectId(project.getId())
                            .userId(memberDto.getUserId())
                            .projectRoleId(roleId)
                            .build();
                    projectMemberRepository.save(member);
                }
            }
        }

        // Create project scheme if template provided
        if (request.getTemplateId() != null) {
            projectSchemeService.createProjectScheme(project, request.getTemplateId());
        } else if (request.getSchemes() != null) {
            // Link existing schemes if provided in import
            projectSchemeService.linkSchemesToProject(project.getId(), request.getSchemes());
        }

        log.info("Project imported successfully: {}", request.getProjectKey());
        return mapToProjectResponse(project);
    }

    @Transactional
    public ProjectResponse cloneProject(UUID sourceProjectId, String newName, String newKey, UUID currentUserId) {
        log.info("Cloning project: {} to new project: {} with key: {}", sourceProjectId, newName, newKey);

        Project source = projectRepository.findById(sourceProjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", sourceProjectId));

        // Validate new key uniqueness
        if (projectRepository.existsByProjectKey(newKey)) {
            throw new DuplicateResourceException("Project with key '" + newKey + "' already exists");
        }

        // Export and re-import
        ProjectExportDto export = exportProject(sourceProjectId);

        ProjectImportRequest importRequest = ProjectImportRequest.builder()
                .name(newName)
                .projectKey(newKey)
                .description(source.getDescription())
                .leadUserId(currentUserId)
                .projectType(source.getProjectType())
                .templateId(source.getTemplateId())
                .category(source.getCategory())
                .avatarUrl(source.getAvatarUrl())
                .defaultAssigneeType(source.getDefaultAssigneeType())
                .allowIssueCreation(source.getAllowIssueCreation())
                .roles(export.getRoles())
                .schemes(export.getSchemes())
                .build();

        ProjectResponse cloned = importProject(importRequest);

        // Add current user as admin
        UUID adminRoleId = projectRoleRepository.findByProjectIdAndName(cloned.getId(), defaultAdminRoleName)
                .map(ProjectRole::getId)
                .orElse(null);

        if (adminRoleId != null) {
            ProjectMember adminMember = ProjectMember.builder()
                    .projectId(cloned.getId())
                    .userId(currentUserId)
                    .projectRoleId(adminRoleId)
                    .build();
            projectMemberRepository.save(adminMember);
        }

        log.info("Project cloned successfully to: {}", newKey);
        return cloned;
    }

    private void createDefaultRoles(UUID projectId, Map<String, UUID> roleIdMap) {
        String[] roleNames = defaultRoleNamesStr.split(",");
        String adminName = roleNames.length > 0 ? roleNames[0].trim() : defaultAdminRoleName;
        String developerName = roleNames.length > 1 ? roleNames[1].trim() : "DEVELOPER";
        String viewerName = roleNames.length > 2 ? roleNames[2].trim() : "VIEWER";

        List<ProjectRole> defaultRoles = Arrays.asList(
                ProjectRole.builder().projectId(projectId).name(adminName)
                        .description(defaultAdminRoleDescription)
                        .permissions(Arrays.asList(defaultAdminPermissions.split(","))).build(),
                ProjectRole.builder().projectId(projectId).name(developerName)
                        .description(defaultDeveloperRoleDescription)
                        .permissions(Arrays.asList(defaultDeveloperPermissions.split(","))).build(),
                ProjectRole.builder().projectId(projectId).name(viewerName)
                        .description(defaultViewerRoleDescription)
                        .permissions(Arrays.asList(defaultViewerPermissions.split(","))).build()
        );

        for (ProjectRole role : defaultRoles) {
            role = projectRoleRepository.save(role);
            roleIdMap.put(role.getName(), role.getId());
        }
    }

    private UUID getEntityId(Object entity) {
        if (entity == null) return null;
        try {
            return (UUID) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectKey(project.getProjectKey())
                .name(project.getName())
                .description(project.getDescription())
                .leadUserId(project.getLeadUserId())
                .projectType(project.getProjectType())
                .templateId(project.getTemplateId())
                .category(project.getCategory())
                .avatarUrl(project.getAvatarUrl())
                .defaultAssigneeType(project.getDefaultAssigneeType())
                .allowIssueCreation(project.getAllowIssueCreation())
                .archived(project.getArchived())
                .archivedAt(project.getArchivedAt())
                .version(project.getVersion())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}