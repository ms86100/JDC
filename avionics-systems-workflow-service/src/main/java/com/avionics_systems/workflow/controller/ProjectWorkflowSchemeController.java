package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.service.ProjectWorkflowSchemeBridgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow-schemes")
@RequiredArgsConstructor
@Tag(name = "Project Workflow Schemes", description = "Assign workflow schemes to projects")
public class ProjectWorkflowSchemeController {

    private final ProjectWorkflowSchemeBridgeService projectWorkflowSchemeBridgeService;
    private final com.avionics_systems.workflow.repository.ProjectWorkflowSchemeRepository projectWorkflowSchemeRepository;

    @PutMapping("/projects/{projectId}/assign")
    @Operation(summary = "Assign workflow scheme to project")
    public ResponseEntity<Map<String, Object>> assignSchemeToProject(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> body) {
        UUID schemeId = UUID.fromString(body.get("schemeId"));
        return ResponseEntity.ok(projectWorkflowSchemeBridgeService.assignSchemeToProject(projectId, schemeId));
    }

    @PostMapping("/projects/assign-bulk")
    @Operation(summary = "Assign workflow scheme to multiple projects")
    public ResponseEntity<Map<String, Object>> assignSchemeBulk(@RequestBody Map<String, Object> body) {
        UUID schemeId = UUID.fromString(String.valueOf(body.get("schemeId")));
        @SuppressWarnings("unchecked")
        List<String> projectIds = (List<String>) body.get("projectIds");
        int updated = projectWorkflowSchemeBridgeService.assignSchemeToProjects(schemeId, projectIds);
        return ResponseEntity.ok(Map.of("schemeId", schemeId.toString(), "updatedProjects", updated));
    }

    @GetMapping("/projects/{projectId}")
    @Operation(summary = "Get workflow scheme for project")
    public ResponseEntity<?> getProjectScheme(@PathVariable UUID projectId) {
        return projectWorkflowSchemeRepository.findById(projectId)
                .map(p -> ResponseEntity.ok(Map.of(
                        "projectId", p.getProjectId().toString(),
                        "schemeId", p.getSchemeId().toString())))
                .orElse(ResponseEntity.notFound().build());
    }
}
