package com.avionics_systems.project.controller;

import com.avionics_systems.project.dto.AssignIssueTypeSchemeRequest;
import com.avionics_systems.project.dto.AssignWorkflowSchemeRequest;
import com.avionics_systems.project.service.ProjectSchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/schemes")
@RequiredArgsConstructor
@Tag(name = "Project Schemes", description = "Assign scheme configuration to projects")
public class ProjectSchemeController {

    private final ProjectSchemeService projectSchemeService;

    @PostMapping("/issue-type/assign")
    @Operation(summary = "Assign issue type scheme to projects (admin bridge)")
    public ResponseEntity<Map<String, Object>> assignIssueTypeScheme(
            @RequestBody AssignIssueTypeSchemeRequest request) {
        int updated = projectSchemeService.assignIssueTypeSchemeFromAdmin(request);
        return ResponseEntity.ok(Map.of(
                "updatedProjects", updated,
                "schemeName", request.getSchemeName() != null ? request.getSchemeName() : ""
        ));
    }

    @PostMapping("/workflow/assign")
    @Operation(summary = "Assign workflow scheme to projects (workflow-service bridge)")
    public ResponseEntity<Map<String, Object>> assignWorkflowScheme(
            @RequestBody AssignWorkflowSchemeRequest request) {
        int updated = projectSchemeService.assignWorkflowSchemeFromBridge(request);
        return ResponseEntity.ok(Map.of(
                "updatedProjects", updated,
                "schemeName", request.getSchemeName() != null ? request.getSchemeName() : ""
        ));
    }
}
