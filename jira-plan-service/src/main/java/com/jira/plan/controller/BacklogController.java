package com.jira.plan.controller;

import com.jira.plan.dto.request.CreatePlanItemRequest;
import com.jira.plan.dto.request.ReorderRequest;
import com.jira.plan.dto.response.BacklogResponse;
import com.jira.plan.dto.response.PlanItemResponse;
import com.jira.plan.service.BacklogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}/backlog")
@RequiredArgsConstructor
public class BacklogController {

    private final BacklogService backlogService;

    @GetMapping
    public ResponseEntity<BacklogResponse> getBacklog(@PathVariable UUID planId) {
        return ResponseEntity.ok(backlogService.getBacklog(planId));
    }

    @PostMapping
    public ResponseEntity<PlanItemResponse> addItemToBacklog(
            @PathVariable UUID planId,
            @Valid @RequestBody CreatePlanItemRequest request) {
        PlanItemResponse response = backlogService.addItemToBacklog(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<PlanItemResponse> updateItem(
            @PathVariable UUID planId,
            @PathVariable UUID itemId,
            @RequestBody CreatePlanItemRequest request) {
        return ResponseEntity.ok(backlogService.updateItem(planId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItemFromBacklog(
            @PathVariable UUID planId,
            @PathVariable UUID itemId) {
        backlogService.removeItemFromBacklog(planId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable UUID planId,
            @RequestBody ReorderRequest request) {
        backlogService.reorderItems(planId, request);
        return ResponseEntity.ok().build();
    }
}
