package com.avionics_systems.project.service;

import com.avionics_systems.project.dto.*;
import com.avionics_systems.project.entity.Project;
import com.avionics_systems.project.entity.ProjectMember;
import com.avionics_systems.project.entity.ProjectRole;
import com.avionics_systems.project.entity.ProjectTemplate;
import com.avionics_systems.project.exception.DuplicateResourceException;
import com.avionics_systems.project.exception.InvalidOperationException;
import com.avionics_systems.project.exception.ResourceNotFoundException;
import com.avionics_systems.project.exception.OptimisticLockException;
import com.avionics_systems.project.repository.ProjectMemberRepository;
import com.avionics_systems.project.repository.ProjectRepository;
import com.avionics_systems.project.repository.ProjectRoleRepository;
import com.avionics_systems.project.repository.ProjectTemplateRepository;
import com.avionics_systems.project.repository.TemplateSchemeDefaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final TemplateSchemeDefaultRepository templateSchemeDefaultRepository;
    private final ProjectSchemeService projectSchemeService;
    private final RestTemplate restTemplate;

    @Value("${issue.service.url}")
    private String issueServiceUrl;

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

    @Value("${app.defaults.permissions.default:read}")
    private String defaultPermissions;

    @Value("${app.defaults.project-key-fallback:PRJ}")
    private String defaultProjectKeyFallback;

    @Value("${app.defaults.project-key-pattern:^[A-Z][A-Z0-9]{1,9}$}")
    private String projectKeyPattern;

    @Transactional
    public ProjectResponse createProjectViaWizard(CreateProjectWizardRequest request, UUID currentUserId) {
        log.info("Creating project via wizard: {} by user: {}", request.getName(), currentUserId);

        try {
            // Validate project key uniqueness
            if (projectRepository.existsByProjectKey(request.getProjectKey())) {
                throw new DuplicateResourceException("Project with key '" + request.getProjectKey() + "' already exists");
            }

            // Get template if provided
            ProjectTemplate template = null;
            String category = null;
            String assigneeType = defaultAssigneeType;
            Boolean allowIssueCreation = true;

            if (request.getTemplateId() != null) {
                template = projectTemplateRepository.findById(request.getTemplateId())
                        .orElse(null);
                if (template != null) {
                    category = template.getName().toLowerCase();
                    assigneeType = template.getDefaultAssigneeType();
                    allowIssueCreation = template.getAllowIssueCreation();
                }
            }

        // Override with explicit values from request if provided
        if (request.getDefaultAssigneeType() != null) {
            assigneeType = request.getDefaultAssigneeType();
        }
        if (request.getAllowIssueCreation() != null) {
            allowIssueCreation = request.getAllowIssueCreation();
        }

        // Build project entity with all new fields
        Project project = Project.builder()
                .projectKey(request.getProjectKey())
                .name(request.getName())
                .description(request.getDescription())
                .leadUserId(request.getLeadUserId() != null ? request.getLeadUserId() : currentUserId)
                .projectType(request.getProjectType())
                .templateId(request.getTemplateId())
                .category(category)
                .avatarUrl(request.getAvatarUrl())
                .defaultAssigneeType(assigneeType)
                .allowIssueCreation(allowIssueCreation)
                .archived(false)
                .build();

        project = projectRepository.save(project);

        // Create default project roles and get the admin role
        ProjectRole adminRole = createProjectRolesAndGetAdmin(project.getId());

        // Assign creator as PROJECT_ADMIN
        ProjectMember member = ProjectMember.builder()
                .projectId(project.getId())
                .userId(currentUserId)
                .projectRoleId(adminRole.getId())
                .build();
        projectMemberRepository.save(member);

        // Create project scheme (links to issue types, workflows, permissions, etc.)
        if (request.getTemplateId() != null) {
            projectSchemeService.createProjectScheme(project, request.getTemplateId());
        }

        log.info("Project created successfully via wizard with key: {}", request.getProjectKey());
        return mapToProjectResponse(project);
        } catch (Exception e) {
            log.error("Error creating project via wizard: {} - {}", request.getProjectKey(), e.getMessage(), e);
            // Re-throw with more context
            if (e instanceof DuplicateResourceException || e instanceof ResourceNotFoundException ||
                e instanceof InvalidOperationException) {
                throw e;
            }
            throw new RuntimeException("Failed to create project: " + e.getMessage(), e);
        }
    }

    // Keep legacy method for backward compatibility
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID currentUserId) {
        log.info("Creating new project (legacy): {} by user: {}", request.getName(), currentUserId);

        String projectKey = request.getProjectKey();
        if (projectKey == null || projectKey.isBlank()) {
            projectKey = generateProjectKey(request.getName());
        } else {
            projectKey = projectKey.trim().toUpperCase();
        }

        if (projectRepository.existsByProjectKey(projectKey)) {
            throw new DuplicateResourceException("Project with key '" + projectKey + "' already exists");
        }

        Project project = Project.builder()
                .projectKey(projectKey)
                .name(request.getName())
                .description(request.getDescription())
                .leadUserId(request.getLeadUserId() != null ? request.getLeadUserId() : currentUserId)
                .projectType(defaultProjectType)
                .build();

        project = projectRepository.save(project);

        // Create project roles and get admin role
        ProjectRole adminRole = createProjectRolesAndGetAdmin(project.getId());

        // Assign creator as PROJECT_ADMIN
        ProjectMember member = ProjectMember.builder()
                .projectId(project.getId())
                .userId(currentUserId)
                .projectRoleId(adminRole.getId())
                .build();
        projectMemberRepository.save(member);

        log.info("Project created successfully with key: {}", projectKey);
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForUser(UUID userId) {
        log.debug("Fetching projects for user: {}", userId);

        if (userId == null) {
            // If no userId, return all projects
            return projectRepository.findAll().stream()
                    .map(this::mapToProjectResponse)
                    .collect(Collectors.toList());
        }

        List<ProjectMember> memberships = projectMemberRepository.findByUserId(userId);

        List<UUID> projectIds = memberships.stream()
                .map(ProjectMember::getProjectId)
                .collect(Collectors.toList());

        if (projectIds.isEmpty()) {
            return List.of();
        }

        return projectRepository.findAllById(projectIds).stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.debug("Fetching all projects");
        return projectRepository.findAll().stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectByKey(String projectKey) {
        Project project = projectRepository.findByProjectKey(projectKey.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "key", projectKey));
        return mapToProjectResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        log.info("Updating project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Optimistic locking: check version if provided
        if (request.getVersion() != null && !request.getVersion().equals(project.getVersion())) {
            throw new OptimisticLockException(
                "Project was modified by another user. Please refresh and try again. " +
                "Expected version: " + project.getVersion() + ", provided: " + request.getVersion());
        }

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getLeadUserId() != null) {
            project.setLeadUserId(request.getLeadUserId());
        }

        project = projectRepository.save(project);
        log.info("Project updated successfully: {}", projectId);
        return mapToProjectResponse(project);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        log.info("Deleting project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Cascade delete related entities to prevent orphaned data
        cascadeDeleteRelatedEntities(projectId);

        // Delete project members first (foreign key constraint)
        projectMemberRepository.deleteByProjectId(projectId);
        log.debug("Deleted project members for project: {}", projectId);

        // Delete project roles
        projectRoleRepository.deleteByProjectId(projectId);
        log.debug("Deleted project roles for project: {}", projectId);

        // Finally delete the project
        projectRepository.delete(project);
        log.info("Project deleted successfully: {}", projectId);
    }

    /**
     * Cascades deletion to related entities in other services.
     * This ensures referential integrity when deleting a project.
     */
    private void cascadeDeleteRelatedEntities(UUID projectId) {
        log.info("Cascading delete for project: {}", projectId);

        // Delete all issues for this project via issue service REST API
        try {
            String issuesUrl = String.format("%s/api/issues/project/%s", issueServiceUrl, projectId);
            restTemplate.delete(issuesUrl);
            log.debug("Deleted issues for project: {}", projectId);
        } catch (Exception e) {
            log.warn("Could not cascade delete issues for project {}: {}", projectId, e.getMessage());
            // Continue with project deletion even if issues deletion fails
            // In production, this should be a distributed transaction
        }

        // Note: In a production system with proper database setup:
        // - Foreign keys would have ON DELETE CASCADE
        // - Or we'd use @OneToMany(cascade = CascadeType.ALL) in entities
        // - Or we'd use distributed transactions (Saga pattern)
    }

    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, ProjectMemberRequest request) {
        log.info("Adding member {} to project {}", request.getUserId(), projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new DuplicateResourceException("User is already a member of this project");
        }

        // Find or create role for this project
        ProjectRole role = projectRoleRepository.findByProjectIdAndName(projectId, request.getProjectRoleName())
                .orElseGet(() -> {
                    // Create the role if it doesn't exist for this project
                    ProjectRole newRole = ProjectRole.builder()
                            .projectId(projectId)
                            .name(request.getProjectRoleName())
                            .permissions(getDefaultPermissionsForRole(request.getProjectRoleName()))
                            .build();
                    return projectRoleRepository.save(newRole);
                });

        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(request.getUserId())
                .projectRoleId(role.getId())
                .build();

        member = projectMemberRepository.save(member);

        return ProjectMemberResponse.builder()
                .projectId(member.getProjectId())
                .userId(member.getUserId())
                .projectRoleId(member.getProjectRoleId())
                .roleName(role.getName())
                .permissions(role.getPermissions())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private List<String> getDefaultPermissionsForRole(String roleName) {
        String[] roleNames = defaultRoleNamesStr.split(",");
        String adminName = roleNames.length > 0 ? roleNames[0].trim() : defaultAdminRoleName;
        String developerName = roleNames.length > 1 ? roleNames[1].trim() : "DEVELOPER";
        String viewerName = roleNames.length > 2 ? roleNames[2].trim() : "VIEWER";

        if (adminName.equals(roleName)) {
            return Arrays.asList(defaultAdminPermissions.split(","));
        } else if (developerName.equals(roleName)) {
            return Arrays.asList(defaultDeveloperPermissions.split(","));
        } else if (viewerName.equals(roleName)) {
            return Arrays.asList(defaultViewerPermissions.split(","));
        }
        return Arrays.asList(defaultPermissions.split(","));
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(UUID projectId) {
        log.debug("Fetching members for project: {}", projectId);

        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        return members.stream().map(member -> {
            ProjectRole role = projectRoleRepository.findById(member.getProjectRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", member.getProjectRoleId()));

            return ProjectMemberResponse.builder()
                    .projectId(member.getProjectId())
                    .userId(member.getUserId())
                    .projectRoleId(member.getProjectRoleId())
                    .roleName(role.getName())
                    .permissions(role.getPermissions())
                    .joinedAt(member.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private ProjectRole createProjectRolesAndGetAdmin(UUID projectId) {
        String[] roleNames = defaultRoleNamesStr.split(",");
        String adminName = roleNames.length > 0 ? roleNames[0].trim() : defaultAdminRoleName;
        String developerName = roleNames.length > 1 ? roleNames[1].trim() : "DEVELOPER";
        String viewerName = roleNames.length > 2 ? roleNames[2].trim() : "VIEWER";

        List<ProjectRole> roles = Arrays.asList(
                ProjectRole.builder()
                        .projectId(projectId)
                        .name(adminName)
                        .description(defaultAdminRoleDescription)
                        .permissions(Arrays.asList(defaultAdminPermissions.split(",")))
                        .build(),
                ProjectRole.builder()
                        .projectId(projectId)
                        .name(developerName)
                        .description(defaultDeveloperRoleDescription)
                        .permissions(Arrays.asList(defaultDeveloperPermissions.split(",")))
                        .build(),
                ProjectRole.builder()
                        .projectId(projectId)
                        .name(viewerName)
                        .description(defaultViewerRoleDescription)
                        .permissions(Arrays.asList(defaultViewerPermissions.split(",")))
                        .build()
        );

        List<ProjectRole> savedRoles = projectRoleRepository.saveAll(roles);
        return savedRoles.stream()
                .filter(r -> adminName.equals(r.getName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Admin role not created"));
    }

    private String generateProjectKey(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            throw new InvalidOperationException("Project name is required to generate key");
        }

        String[] words = projectName.trim().split("\\s+");
        StringBuilder key = new StringBuilder();

        for (String word : words) {
            if (key.length() >= 10) break;
            String cleaned = word.replaceAll("[^a-zA-Z0-9]", "");
            if (!cleaned.isEmpty()) {
                key.append(Character.toUpperCase(cleaned.charAt(0)));
                if (key.length() >= 10) break;
            }
        }

        if (key.isEmpty()) {
            key.append(defaultProjectKeyFallback);
        }

        while (key.length() < 3) {
            key.append("X");
        }

        return key.toString().toUpperCase().substring(0, Math.min(key.length(), 10));
    }

    public boolean isValidProjectKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return key.matches(projectKeyPattern);
    }

    public boolean isProjectKeyAvailable(String key) {
        return key != null && !projectRepository.existsByProjectKey(key);
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
                .defaultAssigneeType(project.getDefaultAssigneeType())
                .allowIssueCreation(project.getAllowIssueCreation())
                .archived(project.getArchived())
                .version(project.getVersion())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}