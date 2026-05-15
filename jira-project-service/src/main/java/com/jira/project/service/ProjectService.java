package com.jira.project.service;

import com.jira.project.dto.*;
import com.jira.project.entity.Project;
import com.jira.project.entity.ProjectMember;
import com.jira.project.entity.ProjectRole;
import com.jira.project.entity.ProjectTemplate;
import com.jira.project.exception.DuplicateResourceException;
import com.jira.project.exception.InvalidOperationException;
import com.jira.project.exception.ResourceNotFoundException;
import com.jira.project.repository.ProjectMemberRepository;
import com.jira.project.repository.ProjectRepository;
import com.jira.project.repository.ProjectRoleRepository;
import com.jira.project.repository.ProjectTemplateRepository;
import com.jira.project.repository.TemplateSchemeDefaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ProjectResponse createProjectViaWizard(CreateProjectWizardRequest request, UUID currentUserId) {
        log.info("Creating project via wizard: {} by user: {}", request.getName(), currentUserId);

        // Validate project key uniqueness
        if (projectRepository.existsByProjectKey(request.getProjectKey())) {
            throw new DuplicateResourceException("Project with key '" + request.getProjectKey() + "' already exists");
        }

        // Get template if provided
        ProjectTemplate template = null;
        String category = null;
        String defaultAssigneeType = "PROJECT_LEAD";
        Boolean allowIssueCreation = true;

        if (request.getTemplateId() != null) {
            template = projectTemplateRepository.findById(request.getTemplateId())
                    .orElse(null);
            if (template != null) {
                category = template.getName().toLowerCase();
                defaultAssigneeType = template.getDefaultAssigneeType();
                allowIssueCreation = template.getAllowIssueCreation();
            }
        }

        // Override with explicit values from request if provided
        if (request.getDefaultAssigneeType() != null) {
            defaultAssigneeType = request.getDefaultAssigneeType();
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
                .defaultAssigneeType(defaultAssigneeType)
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
    }

    // Keep legacy method for backward compatibility
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID currentUserId) {
        log.info("Creating new project (legacy): {} by user: {}", request.getName(), currentUserId);

        String projectKey = generateProjectKey(request.getName());

        if (projectRepository.existsByProjectKey(projectKey)) {
            throw new DuplicateResourceException("Project with key '" + projectKey + "' already exists");
        }

        Project project = Project.builder()
                .projectKey(projectKey)
                .name(request.getName())
                .description(request.getDescription())
                .leadUserId(request.getLeadUserId() != null ? request.getLeadUserId() : currentUserId)
                .projectType("COMPANY_MANAGED")
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
    public ProjectResponse getProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return mapToProjectResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        log.info("Updating project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

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

        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }

        projectRepository.deleteById(projectId);
        log.info("Project deleted successfully: {}", projectId);
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
        return switch (roleName) {
            case "PROJECT_ADMIN" -> List.of("*");
            case "DEVELOPER" -> List.of("read", "edit", "create", "comment", "transition");
            case "VIEWER" -> List.of("read", "comment");
            default -> List.of("read");
        };
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
        List<ProjectRole> roles = Arrays.asList(
                ProjectRole.builder()
                        .projectId(projectId)
                        .name("PROJECT_ADMIN")
                        .description("Project Administrator with full access")
                        .permissions(List.of("*"))
                        .build(),
                ProjectRole.builder()
                        .projectId(projectId)
                        .name("DEVELOPER")
                        .description("Developer with edit and create permissions")
                        .permissions(List.of("read", "edit", "create", "comment", "transition"))
                        .build(),
                ProjectRole.builder()
                        .projectId(projectId)
                        .name("VIEWER")
                        .description("Read-only access")
                        .permissions(List.of("read", "comment"))
                        .build()
        );

        List<ProjectRole> savedRoles = projectRoleRepository.saveAll(roles);
        return savedRoles.stream()
                .filter(r -> "PROJECT_ADMIN".equals(r.getName()))
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
            key.append("PRJ");
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
        return key.matches("^[A-Z][A-Z0-9]{1,9}$");
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
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}