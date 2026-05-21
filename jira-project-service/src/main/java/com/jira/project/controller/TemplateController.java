package com.jira.project.controller;

import com.jira.project.dto.*;
import com.jira.project.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TemplateController - Handles template-related API endpoints
 *
 * Provides endpoints for:
 * - Listing templates by category (for Create Project modal)
 * - Getting template details with workflow visualization
 * - Listing available statuses for workflow configuration
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Project template management endpoints")
public class TemplateController {

    private final TemplateService templateService;

    /**
     * Full template catalog for Create Project wizard (categories, capabilities, recommended).
     */
    @GetMapping("/catalog")
    @Operation(summary = "Get template catalog",
            description = "Returns Jira DC-style template catalog with categories, capabilities, and recommended templates")
    public ResponseEntity<TemplateCatalogResponse> getTemplateCatalog() {
        return ResponseEntity.ok(templateService.getTemplateCatalog());
    }

    /**
     * Get all templates grouped by category
     * This is used by the Create Project modal to show template selection
     */
    @GetMapping("/categories")
    @Operation(summary = "Get templates by category",
            description = "Returns all active templates grouped by category for the Create Project modal")
    public ResponseEntity<List<TemplateCategoryResponse>> getTemplatesByCategory() {
        List<TemplateCategoryResponse> categories = templateService.getTemplatesByCategory();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get templates for a specific category
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get templates for category",
            description = "Returns all active templates for a specific category")
    public ResponseEntity<List<ProjectTemplateResponse>> getTemplatesByCategoryName(
            @Parameter(description = "Category name (BUSINESS, SOFTWARE, TEAM_MANAGED)")
            @PathVariable String category) {
        List<ProjectTemplateResponse> templates = templateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get templates for a specific project type
     */
    @GetMapping("/type/{typeId}")
    @Operation(summary = "Get templates for project type",
            description = "Returns all active templates for a specific project type")
    public ResponseEntity<List<ProjectTemplateResponse>> getTemplatesByType(
            @Parameter(description = "Project Type ID") @PathVariable UUID typeId) {
        List<ProjectTemplateResponse> templates = templateService.getTemplatesByType(typeId);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get template with full workflow details
     * This is used by the template detail modal to show workflow preview
     */
    @GetMapping("/{templateId}/workflow")
    @Operation(summary = "Get template with workflow visualization",
            description = "Returns full template details including issue types, workflow statuses, and transitions for workflow preview")
    public ResponseEntity<TemplateWithWorkflowResponse> getTemplateWithWorkflow(
            @Parameter(description = "Template ID") @PathVariable UUID templateId) {
        TemplateWithWorkflowResponse template = templateService.getTemplateWithWorkflow(templateId);
        return ResponseEntity.ok(template);
    }

    /**
     * Get simple template details
     */
    @GetMapping("/{templateId}")
    @Operation(summary = "Get template details",
            description = "Returns basic template information without workflow details")
    public ResponseEntity<ProjectTemplateResponse> getTemplate(
            @Parameter(description = "Template ID") @PathVariable UUID templateId) {
        ProjectTemplateResponse template = templateService.getTemplate(templateId);
        return ResponseEntity.ok(template);
    }

    /**
     * Get all templates
     */
    @GetMapping
    @Operation(summary = "Get all templates",
            description = "Returns all templates (active and inactive)")
    public ResponseEntity<List<ProjectTemplateResponse>> getAllTemplates() {
        List<ProjectTemplateResponse> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Get available workflow statuses
     * This returns the system-wide status definitions that can be used in templates
     */
    @GetMapping("/workflows/available-statuses")
    @Operation(summary = "Get available workflow statuses",
            description = "Returns all available workflow statuses that can be used when configuring template workflows")
    public ResponseEntity<List<TemplateWithWorkflowResponse.TemplateWorkflowStatusDto>> getAvailableStatuses() {
        List<TemplateWithWorkflowResponse.TemplateWorkflowStatusDto> statuses = templateService.getAvailableStatuses();
        return ResponseEntity.ok(statuses);
    }
}