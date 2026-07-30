package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.ComponentResponse;
import com.avionics_systems.issue.dto.CreateComponentRequest;
import com.avionics_systems.issue.dto.UpdateComponentRequest;
import com.avionics_systems.issue.service.ComponentService;
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
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Tag(name = "Components", description = "Project component management endpoints")
public class ComponentController {

    private final ComponentService componentService;

    @PostMapping
    @Operation(summary = "Create a new component", description = "Creates a new component in the specified project")
    public ResponseEntity<ComponentResponse> createComponent(
            @Valid @RequestBody CreateComponentRequest request) {

        ComponentResponse response = componentService.createComponent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get components for project", description = "Returns all components for a project")
    public ResponseEntity<List<ComponentResponse>> getComponentsForProject(
            @Parameter(description = "Project ID") @RequestParam UUID projectId) {

        List<ComponentResponse> components = componentService.getComponentsForProject(projectId);
        return ResponseEntity.ok(components);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get component by ID", description = "Returns component details by ID")
    public ResponseEntity<ComponentResponse> getComponent(
            @Parameter(description = "Component ID") @PathVariable UUID id) {

        ComponentResponse response = componentService.getComponent(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update component", description = "Updates component details")
    public ResponseEntity<ComponentResponse> updateComponent(
            @Parameter(description = "Component ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateComponentRequest request) {

        ComponentResponse response = componentService.updateComponent(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete component", description = "Deletes a component")
    public ResponseEntity<Void> deleteComponent(
            @Parameter(description = "Component ID") @PathVariable UUID id) {

        componentService.deleteComponent(id);
        return ResponseEntity.noContent().build();
    }
}