package com.jira.issue.controller;

import com.jira.issue.dto.bulk.BulkOperationRequest;
import com.jira.issue.dto.bulk.BulkOperationResponse;
import com.jira.issue.service.BulkIssueOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bulk-operations")
@RequiredArgsConstructor
@Tag(name = "Bulk Operations", description = "Jira DC-style bulk change on issues")
public class BulkOperationController {

    private final BulkIssueOperationService bulkIssueOperationService;

    @PostMapping
    @Operation(summary = "Execute bulk operation on issues")
    public ResponseEntity<BulkOperationResponse> execute(
            @Valid @RequestBody BulkOperationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(bulkIssueOperationService.execute(request, userId));
    }
}
