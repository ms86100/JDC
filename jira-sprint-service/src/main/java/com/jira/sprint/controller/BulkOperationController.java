package com.jira.sprint.controller;

import com.jira.sprint.dto.BulkOperationRequest;
import com.jira.sprint.dto.BulkOperationResponse;
import com.jira.sprint.service.BulkOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bulk-operations")
@RequiredArgsConstructor
@Tag(name = "Bulk Operations", description = "Bulk issue operations API")
public class BulkOperationController {

    private final BulkOperationService bulkOperationService;

    @PostMapping
    @Operation(summary = "Execute bulk operation", description = "Execute a bulk operation on multiple issues")
    public ResponseEntity<BulkOperationResponse> executeBulkOperation(
            @RequestBody BulkOperationRequest request) {
        return ResponseEntity.ok(bulkOperationService.executeBulkOperation(request));
    }

    @GetMapping("/{operationId}")
    @Operation(summary = "Get operation status", description = "Get the status of a bulk operation")
    public ResponseEntity<BulkOperationResponse> getOperationStatus(
            @PathVariable String operationId) {
        BulkOperationResponse response = bulkOperationService.getOperationStatus(operationId);
        return response != null
                ? ResponseEntity.ok(response)
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent operations", description = "Get recent bulk operations")
    public ResponseEntity<List<BulkOperationResponse>> getRecentOperations() {
        return ResponseEntity.ok(bulkOperationService.getRecentOperations());
    }
}