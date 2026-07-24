package com.jira.admin.controller;

import com.jira.admin.dto.*;
import com.jira.admin.service.PermissionSchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Permission Scheme Controller - REST API for permission scheme management.
 * Provides endpoints for CRUD operations on permission schemes and grants.
 */
@RestController
@RequestMapping("/api/admin/permission-schemes")
@RequiredArgsConstructor
@Tag(name = "Permission Schemes", description = "Permission Scheme Management API")
public class PermissionSchemeController {

    private final PermissionSchemeService permissionSchemeService;

    // ==================== Permission Scheme CRUD ====================

    @GetMapping
    @Operation(summary = "List all permission schemes",
            description = "Returns a list of all permission schemes with their basic information")
    public ResponseEntity<List<PermissionSchemeDto>> getAllPermissionSchemes() {
        List<PermissionSchemeDto> schemes = permissionSchemeService.getAllPermissionSchemes();
        return ResponseEntity.ok(schemes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get permission scheme by ID",
            description = "Returns detailed information about a specific permission scheme including all grants")
    public ResponseEntity<PermissionSchemeDto> getPermissionSchemeById(@PathVariable String id) {
        return permissionSchemeService.getPermissionSchemeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new permission scheme",
            description = "Creates a new permission scheme with the provided configuration")
    public ResponseEntity<PermissionSchemeDto> createPermissionScheme(
            @Valid @RequestBody CreatePermissionSchemeRequest request) {
        try {
            PermissionSchemeDto created = permissionSchemeService.createPermissionScheme(request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a permission scheme",
            description = "Updates an existing permission scheme with new configuration")
    public ResponseEntity<PermissionSchemeDto> updatePermissionScheme(
            @PathVariable String id,
            @Valid @RequestBody CreatePermissionSchemeRequest request) {
        try {
            return permissionSchemeService.updatePermissionScheme(id, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a permission scheme",
            description = "Deletes a permission scheme and all its associated grants")
    public ResponseEntity<Void> deletePermissionScheme(@PathVariable String id) {
        boolean deleted = permissionSchemeService.deletePermissionScheme(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ==================== Permission Grants ====================

    @GetMapping("/{id}/grants")
    @Operation(summary = "Get all grants for a permission scheme",
            description = "Returns all permission grants associated with a specific permission scheme")
    public ResponseEntity<List<PermissionGrantDto>> getGrantsForScheme(@PathVariable String id) {
        return permissionSchemeService.getPermissionSchemeById(id)
                .map(scheme -> {
                    List<PermissionGrantDto> grants = permissionSchemeService.getGrantsForScheme(id);
                    return ResponseEntity.ok(grants);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/grants")
    @Operation(summary = "Add a permission grant to a scheme",
            description = "Adds a new permission grant to the specified permission scheme")
    public ResponseEntity<PermissionGrantDto> addPermissionGrant(
            @PathVariable String id,
            @Valid @RequestBody CreatePermissionGrantRequest request) {
        try {
            return permissionSchemeService.addPermissionGrant(id, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/grants/{grantId}")
    @Operation(summary = "Remove a permission grant",
            description = "Removes a specific permission grant from a permission scheme")
    public ResponseEntity<Void> removePermissionGrant(
            @PathVariable String id,
            @PathVariable String grantId) {
        boolean removed = permissionSchemeService.removePermissionGrant(id, grantId);
        if (removed) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}