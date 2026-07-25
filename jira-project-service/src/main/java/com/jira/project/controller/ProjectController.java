package com.jira.project.controller;

import com.jira.project.dto.*;
import com.jira.project.entity.Permission;
import com.jira.project.service.ProjectService;
import com.jira.project.service.ProjectTypeService;
import com.jira.project.service.ProjectSchemeService;
import com.jira.project.service.PermissionCheckService;
import com.jira.project.service.ArchiveService;
import com.jira.project.service.ExportImportService;
import com.jira.project.service.BulkProjectService;
import com.jira.project.exception.PermissionDeniedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectTypeService projectTypeService;
    private final ProjectSchemeService projectSchemeService;
    private final PermissionCheckService permissionCheckService;
    private final ArchiveService archiveService;
    private final ExportImportService exportImportService;
    private final BulkProjectService bulkProjectService;

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

    @GetMapping("/{id}/schemes")
    @Operation(summary = "Get project scheme IDs bundle", description = "Returns compact IDs for all schemes assigned to the project")
    public ResponseEntity<ProjectSchemesBundleResponse> getProjectSchemesBundle(
            @Parameter(description = "Project ID") @PathVariable UUID id) {
        return ResponseEntity.ok(projectSchemeService.getSchemesBundle(id));
    }

    @GetMapping("/{id}/scheme/screens")
    @Operation(summary = "Resolve screen IDs for issue type", description = "Returns create/edit/view screen IDs from the project screen scheme")
    public ResponseEntity<ProjectScreenResolutionResponse> resolveProjectScreens(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID issueTypeId) {
        return ResponseEntity.ok(projectSchemeService.resolveScreens(id, issueTypeId));
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
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        List<ProjectResponse> projects = projectService.getProjectsForUser(userId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/all")
    @Operation(summary = "List all projects", description = "Returns all projects (admin endpoint, no user filtering)")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID", description = "Returns project details by ID")
    public ResponseEntity<ProjectResponse> getProject(
            @Parameter(description = "Project ID") @PathVariable UUID id) {

        ProjectResponse response = projectService.getProject(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{projectKey}")
    @Operation(summary = "Get project by key", description = "Returns project details by project key")
    public ResponseEntity<ProjectResponse> getProjectByKey(
            @Parameter(description = "Project key") @PathVariable String projectKey) {

        ProjectResponse response = projectService.getProjectByKey(projectKey);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project", description = "Updates project details")
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Check ADMINISTER_PROJECTS permission before allowing update
        if (userId != null && !permissionCheckService.canAdministerProject(userId, id)) {
            throw new PermissionDeniedException(Permission.ADMINISTER_PROJECTS, "project " + id);
        }

        ProjectResponse response = projectService.updateProject(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Deletes a project and all associated data")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Check ADMINISTER_PROJECTS permission before allowing delete
        if (userId != null && !permissionCheckService.canAdministerProject(userId, id)) {
            throw new PermissionDeniedException(Permission.ADMINISTER_PROJECTS, "project " + id);
        }

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/archived")
    @Operation(summary = "List archived projects", description = "Returns all archived projects")
    public ResponseEntity<List<ProjectResponse>> getArchivedProjects() {
        List<ProjectResponse> projects = archiveService.getArchivedProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/active")
    @Operation(summary = "List active projects", description = "Returns all non-archived projects")
    public ResponseEntity<List<ProjectResponse>> getActiveProjects() {
        List<ProjectResponse> projects = archiveService.getActiveProjects();
        return ResponseEntity.ok(projects);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive project", description = "Archives a project making it read-only")
    public ResponseEntity<ProjectResponse> archiveProject(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        if (userId != null && !permissionCheckService.canAdministerProject(userId, id)) {
            throw new PermissionDeniedException(Permission.ADMINISTER_PROJECTS, "project " + id);
        }

        ProjectResponse response = archiveService.archiveProject(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore archived project", description = "Restores an archived project to active state")
    public ResponseEntity<ProjectResponse> restoreProject(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        if (userId != null && !permissionCheckService.canAdministerProject(userId, id)) {
            throw new PermissionDeniedException(Permission.ADMINISTER_PROJECTS, "project " + id);
        }

        ProjectResponse response = archiveService.restoreProject(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone project", description = "Creates a copy of a project with a new name and key")
    public ResponseEntity<ProjectResponse> cloneProject(
            @Parameter(description = "Source Project ID") @PathVariable UUID id,
            @Valid @RequestBody CloneProjectRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        ProjectResponse response = exportImportService.cloneProject(id, request.getName(), request.getProjectKey(), userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export project", description = "Exports project configuration for import into another project")
    public ResponseEntity<ProjectExportDto> exportProject(
            @Parameter(description = "Project ID") @PathVariable UUID id) {

        ProjectExportDto export = exportImportService.exportProject(id);
        return ResponseEntity.ok(export);
    }

    @PostMapping("/import")
    @Operation(summary = "Import project", description = "Imports a project from exported configuration")
    public ResponseEntity<ProjectResponse> importProject(
            @Valid @RequestBody ProjectImportRequest request) {

        ProjectResponse response = exportImportService.importProject(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add project member", description = "Adds a user as a member of the project with a specific role")
    public ResponseEntity<ProjectMemberResponse> addMember(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @Valid @RequestBody ProjectMemberRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Check ADMINISTER_PROJECTS permission before allowing member addition
        if (userId != null && !permissionCheckService.canAdministerProject(userId, id)) {
            throw new PermissionDeniedException(Permission.ADMINISTER_PROJECTS, "project " + id);
        }

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

    @GetMapping("/{id}/permissions/check")
    @Operation(summary = "Check user permission", description = "Check if a user has a specific permission in this project")
    public ResponseEntity<Map<String, Object>> checkPermission(
            @Parameter(description = "Project ID") @PathVariable UUID id,
            @Parameter(description = "User ID") @RequestParam UUID userId,
            @Parameter(description = "Permission key") @RequestParam String permission) {

        boolean hasPermission = permissionCheckService.hasPermission(userId, id, permission);
        return ResponseEntity.ok(Map.of("hasPermission", hasPermission));
    }

    @PostMapping("/bulk/archive")
    @Operation(summary = "Bulk archive projects", description = "Archives multiple projects at once")
    public ResponseEntity<BulkProjectResponse> bulkArchiveProjects(
            @Valid @RequestBody BulkArchiveRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        BulkProjectResponse response = bulkProjectService.bulkArchive(request.getProjectIds());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk/restore")
    @Operation(summary = "Bulk restore projects", description = "Restores multiple archived projects at once")
    public ResponseEntity<BulkProjectResponse> bulkRestoreProjects(
            @Valid @RequestBody BulkArchiveRequest request) {

        BulkProjectResponse response = bulkProjectService.bulkRestore(request.getProjectIds());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk/delete")
    @Operation(summary = "Bulk delete projects", description = "Deletes multiple projects at once")
    public ResponseEntity<BulkProjectResponse> bulkDeleteProjects(
            @Valid @RequestBody BulkDeleteRequest request) {

        BulkProjectResponse response = bulkProjectService.bulkDelete(request.getProjectIds());
        return ResponseEntity.ok(response);
    }
}