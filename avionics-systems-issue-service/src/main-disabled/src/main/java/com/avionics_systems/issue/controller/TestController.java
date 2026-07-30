package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.TestManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Test Controller - Native test management as Avionics Systems issues
 */
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Tag(name = "Test Management", description = "Native test management APIs")
public class TestController {

    private final TestManagementService testService;

    @PostMapping
    @Operation(summary = "Create a new test")
    public ResponseEntity<TestResponse> createTest(
            @RequestParam UUID projectId,
            @RequestBody CreateTestRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.createTest(projectId, request, userId));
    }

    @GetMapping("/{testId}")
    @Operation(summary = "Get a test by ID")
    public ResponseEntity<TestResponse> getTest(@PathVariable UUID testId) {
        return ResponseEntity.ok(testService.getTest(testId));
    }

    @GetMapping
    @Operation(summary = "Get all tests for a project")
    public ResponseEntity<List<TestResponse>> getTestsByProject(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String testType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID folderId) {
        return ResponseEntity.ok(testService.getTestsByProject(projectId, testType, status, folderId));
    }

    @PutMapping("/{testId}")
    @Operation(summary = "Update a test")
    public ResponseEntity<TestResponse> updateTest(
            @PathVariable UUID testId,
            @RequestBody CreateTestRequest request) {
        return ResponseEntity.ok(testService.updateTest(testId, request));
    }

    @DeleteMapping("/{testId}")
    @Operation(summary = "Delete (archive) a test")
    public ResponseEntity<Void> deleteTest(@PathVariable UUID testId) {
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Test Repository Folders ====================

    @PostMapping("/folders")
    @Operation(summary = "Create a test repository folder")
    public ResponseEntity<TestRepositoryFolderResponse> createFolder(
            @RequestParam UUID projectId,
            @RequestBody CreateFolderRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.createFolder(projectId, request, userId));
    }

    @GetMapping("/folders")
    @Operation(summary = "Get all folders for a project")
    public ResponseEntity<List<TestRepositoryFolderResponse>> getFolders(@RequestParam UUID projectId) {
        return ResponseEntity.ok(testService.getFoldersByProject(projectId));
    }

    @PutMapping("/{testId}/folder")
    @Operation(summary = "Move test to a folder")
    public ResponseEntity<Void> moveTestToFolder(
            @PathVariable UUID testId,
            @RequestParam UUID folderId) {
        testService.moveTestToFolder(testId, folderId);
        return ResponseEntity.ok().build();
    }
}