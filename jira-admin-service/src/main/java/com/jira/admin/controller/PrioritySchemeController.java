package com.jira.admin.controller;

import com.jira.admin.entity.PrioritySchemeEntity;
import com.jira.admin.entity.PrioritySchemeItemEntity;
import com.jira.admin.service.PrioritySchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/priority-schemes")
@RequiredArgsConstructor
@Tag(name = "Priority Schemes", description = "Priority Scheme Management API")
public class PrioritySchemeController {

    private final PrioritySchemeService prioritySchemeService;

    @GetMapping
    @Operation(summary = "List all priority schemes")
    public ResponseEntity<List<Map<String, Object>>> getAllSchemes() {
        return ResponseEntity.ok(prioritySchemeService.getAllSchemes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get priority scheme by ID")
    public ResponseEntity<Map<String, Object>> getSchemeById(@PathVariable String id) {
        return prioritySchemeService.getSchemeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new priority scheme")
    public ResponseEntity<PrioritySchemeEntity> createScheme(@RequestBody PrioritySchemeEntity scheme) {
        PrioritySchemeEntity created = prioritySchemeService.createScheme(scheme);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a priority scheme")
    public ResponseEntity<PrioritySchemeEntity> updateScheme(@PathVariable String id,
                                                              @RequestBody PrioritySchemeEntity scheme) {
        return prioritySchemeService.updateScheme(id, scheme)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a priority scheme")
    public ResponseEntity<Void> deleteScheme(@PathVariable String id) {
        if (prioritySchemeService.deleteScheme(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/projects/{projectId}")
    @Operation(summary = "Assign priority scheme to a project")
    public ResponseEntity<PrioritySchemeEntity> assignToProject(@PathVariable String id,
                                                                 @PathVariable String projectId) {
        try {
            PrioritySchemeEntity scheme = prioritySchemeService.assignSchemeToProject(projectId, id);
            return ResponseEntity.ok(scheme);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get priority scheme assigned to a project")
    public ResponseEntity<Map<String, Object>> getProjectScheme(@PathVariable String projectId) {
        return prioritySchemeService.getProjectPriorityScheme(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/priorities")
    @Operation(summary = "Set priorities for a scheme")
    public ResponseEntity<List<PrioritySchemeItemEntity>> setSchemeItems(
            @PathVariable String id,
            @RequestBody List<PrioritySchemeItemEntity> items) {
        try {
            List<PrioritySchemeItemEntity> saved = prioritySchemeService.setSchemeItems(id, items);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
