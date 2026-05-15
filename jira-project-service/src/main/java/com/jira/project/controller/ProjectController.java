package com.jira.project.controller;

import com.jira.project.dto.*;
import com.jira.project.service.ProjectService;
import com.jira.project.service.ProjectTypeService;
import com.jira.project.service.ProjectSchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectTypeService projectTypeService;
    private final ProjectSchemeService projectSchemeService;

    @PostMapping("/wizard")
    @Operation(summary = "Create project via wizard", description = "Creates a new project using the multi-step wizard flow with full configuration")
    public ResponseEntity<ProjectResponse> createProjectViaWizard(
            @Valid @RequestBody CreateProjectWizardRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        ProjectResponse response = projectService.createProjectViaWizard(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/types")
    @Operation(summary = "List project types", description = "Returns all active project types (Company-managed, Team-managed)")
    public ResponseEntity<List<ProjectTypeResponse>> getProjectTypes() {
        List<ProjectTypeResponse> types = projectTypeService.getAllActiveTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/types/{typeId}")
    @Operation(summary = "Get project type by ID", description = "Returns details for a specific project type")
    public ResponseEntity<ProjectTypeResponse> getProjectType(
            @Parameter(description = "Project Type ID") @PathVariable UUID typeId) {
        ProjectTypeResponse type = projectTypeService.getTypeById(typeId);
        return ResponseEntity.ok(type);
    }

    @GetMapping("/types/{typeId}/templates")
    @Operation(summary = "List templates for type", description = "Returns all templates available for a project type")
    public ResponseEntity<List<ProjectTemplateResponse>> getTemplatesForType(
            @Parameter(description = "Project Type ID") @PathVariable UUID typeId) {
        List<ProjectTemplateResponse> templates = projectTypeService.getTemplatesForType(typeId);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/{templateId}")
    @Operation(summary = "Get template details", description = "Returns full details of a template including default scheme assignments")
    public ResponseEntity<TemplateDetailsResponse> getTemplateDetails(
            @Parameter(description = "Template ID") @PathVariable UUID templateId) {
        TemplateDetailsResponse details = projectTypeService.getTemplateDetails(templateId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/{id}/scheme")
    @Operation(summary = "Get project scheme configuration", description = "Returns the scheme configuration for a project (issue types, workflows, permissions)")
    public ResponseEntity<ProjectSchemeResponse> getProjectScheme(
            @Parameter(description = "Project ID") @PathVariable UUID id) {
        ProjectSchemeResponse scheme = projectSchemeService.getSchemeByProjectId(id);
        return ResponseEntity.ok(scheme);
    }

    @GetMapping("/key/check/{key}")
    @Operation(summary = "Check project key availability", description = "Validates if a project key is available and follows format rules")
    public ResponseEntity<ProjectKeyCheckResponse> checkProjectKey(
            @Parameter(description = "Project Key to check") @PathVariable String key) {
        boolean isValid = projectService.isValidProjectKey(key);
        boolean isAvailable = projectService.isProjectKeyAvailable(key);
        return ResponseEntity.ok(new ProjectKeyCheckResponse(key, isValid, isAvailable));
    }

    @PostMapping
    @Operation(summary = "Create a new project", description = "Creates a new project and assigns the creator as PROJECT_ADMIN")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        ProjectResponse response = projectService.createProject(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all projects for user", description = "Returns all projects the current user is a member of")
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @RequestHeader("X-User-Id") UUID userId) {

        List<ProjectResponse> projects = projectService.getProjectsForUser(userId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Returns project details by ID")
    public ResponseEntity<ProjectResponse> getProject(
            @Parameter(description = "Project ID") @PathVariable UUID id) {

        ProjectResponse response = projectService.getProject(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project", description = "Updates project details")
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {

        ProjectResponse response = projectService.updateProject(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Deletes a project and all associated data")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "Project ID") @PathVariable UUID id) {

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add project member", description = "Adds a user as a member of the project with a specific role")
    public ResponseEntity<ProjectMemberResponse> addMember(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @Valid @RequestBody ProjectMemberRequest request) {

        ProjectMemberResponse response = projectService.addMember(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List project members", description = "Returns all members of the project")
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(
            @Parameter(description = "Project ID") @PathVariable UUID id) {

        List<ProjectMemberResponse> members = projectService.getProjectMembers(id);
        return ResponseEntity.ok(members);
    }
}