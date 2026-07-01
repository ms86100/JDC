package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.FolderStatsService;
import com.jira.test.service.TestFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-folders")
@RequiredArgsConstructor
@Tag(name = "Test Folder Management", description = "APIs for managing test folder hierarchy")
public class TestFolderController {

    private final TestFolderService folderService;
    private final FolderStatsService folderStatsService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new folder")
    public ResponseEntity<FolderResponse> createFolder(@Valid @RequestBody CreateFolderRequest request) {
        FolderResponse folder = folderService.createFolder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(folder);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a folder by ID")
    public ResponseEntity<FolderResponse> getFolder(@PathVariable UUID id, @RequestParam UUID projectId) {
        FolderResponse folder = folderService.getById(id);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all folders for a project (flat list)")
    public ResponseEntity<List<FolderResponse>> getFoldersByProject(@PathVariable UUID projectId) {
        List<FolderResponse> folders = folderService.getFolderTree(projectId);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/project/{projectId}/tree")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get folder tree structure for a project")
    public ResponseEntity<List<FolderResponse>> getFolderTree(@PathVariable UUID projectId) {
        List<FolderResponse> folders = folderService.getFolderTree(projectId);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/project/{projectId}/root")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get root folders only for a project")
    public ResponseEntity<List<FolderResponse>> getRootFolders(@PathVariable UUID projectId) {
        List<FolderResponse> folders = folderService.getRootFolders(projectId);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/parent/{parentId}/children")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get child folders by parent ID")
    public ResponseEntity<List<FolderResponse>> getChildFolders(@PathVariable UUID parentId, @RequestParam UUID projectId) {
        List<FolderResponse> folders = folderService.getChildFolders(parentId);
        return ResponseEntity.ok(folders);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Update a folder")
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @Valid @RequestBody UpdateFolderRequest request) {
        FolderResponse folder = folderService.updateFolder(id, request);
        return ResponseEntity.ok(folder);
    }

    @PutMapping("/{id}/move")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Move a folder to a new parent")
    public ResponseEntity<FolderResponse> moveFolder(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestParam(required = false) UUID newParentId,
            @RequestParam(required = false) Integer sortOrder) {
        FolderResponse folder = folderService.moveFolder(id, newParentId, sortOrder);
        return ResponseEntity.ok(folder);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Delete a folder")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID id, @RequestParam UUID projectId) {
        folderService.deleteFolder(id, false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}/starred")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get starred folders for a project")
    public ResponseEntity<List<FolderResponse>> getStarredFolders(@PathVariable UUID projectId) {
        List<FolderResponse> folders = folderService.getStarredFolders(projectId);
        return ResponseEntity.ok(folders);
    }

    @PutMapping("/{id}/star")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Toggle star on a folder")
    public ResponseEntity<FolderResponse> toggleStar(@PathVariable UUID id, @RequestParam UUID projectId) {
        FolderResponse folder = folderService.toggleStar(id);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/project/{projectId}/type/{type}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get folders by type for a project")
    public ResponseEntity<List<FolderResponse>> getFoldersByType(
            @PathVariable UUID projectId,
            @PathVariable String type) {
        List<FolderResponse> folders = folderService.getFoldersByType(projectId, type);
        return ResponseEntity.ok(folders);
    }

    // ==================== Folder Stats ====================

    @GetMapping("/{id}/stats")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get statistics for a folder")
    public ResponseEntity<FolderStatsResponse> getFolderStats(@PathVariable UUID id, @RequestParam UUID projectId) {
        FolderStatsResponse stats = folderStatsService.getFolderStats(id);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/project/{projectId}/stats")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get statistics for all folders in a project")
    public ResponseEntity<List<FolderStatsResponse>> getAllFolderStats(@PathVariable UUID projectId) {
        List<FolderStatsResponse> stats = folderStatsService.getAllFolderStats(projectId);
        return ResponseEntity.ok(stats);
    }
}