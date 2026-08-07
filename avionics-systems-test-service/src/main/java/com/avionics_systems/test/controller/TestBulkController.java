package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.BulkTestRequest;
import com.avionics_systems.test.dto.BulkTestResponse;
import com.avionics_systems.test.service.TestBulkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tests/bulk")
@RequiredArgsConstructor
@Tag(name = "Test Bulk Operations", description = "APIs for bulk test operations")
public class TestBulkController {

    private final TestBulkService testBulkService;

    @PutMapping("/status")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Bulk update test status")
    public ResponseEntity<BulkTestResponse> bulkUpdateStatus(
            @RequestParam UUID projectId,
            @Valid @RequestBody BulkTestRequest request) {
        BulkTestResponse response = testBulkService.bulkUpdateStatus(request.getTestIds(), request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/assign")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Bulk assign tests to owner")
    public ResponseEntity<BulkTestResponse> bulkAssign(
            @RequestParam UUID projectId,
            @Valid @RequestBody BulkTestRequest request) {
        BulkTestResponse response = testBulkService.bulkAssign(request.getTestIds(), request.getOwnerId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/move-folder")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Bulk move tests to folder")
    public ResponseEntity<BulkTestResponse> bulkMoveToFolder(
            @RequestParam UUID projectId,
            @Valid @RequestBody BulkTestRequest request) {
        BulkTestResponse response = testBulkService.bulkMoveToFolder(request.getTestIds(), request.getFolderId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/add-to-set")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Bulk add tests to test set")
    public ResponseEntity<BulkTestResponse> bulkAddToTestSet(
            @RequestParam UUID projectId,
            @Valid @RequestBody BulkTestRequest request) {
        BulkTestResponse response = testBulkService.bulkAddToTestSet(request.getTestIds(), request.getTestSetId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/labels")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Bulk add labels to tests")
    public ResponseEntity<BulkTestResponse> bulkAddLabels(
            @RequestParam UUID projectId,
            @Valid @RequestBody BulkTestRequest request) {
        BulkTestResponse response = testBulkService.bulkAddLabels(request.getTestIds(), request.getLabels());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Bulk delete (archive) tests")
    public ResponseEntity<BulkTestResponse> bulkDelete(
            @RequestParam UUID projectId,
            @Valid @RequestBody BulkTestRequest request) {
        BulkTestResponse response = testBulkService.bulkDelete(request.getTestIds());
        return ResponseEntity.ok(response);
    }
}
