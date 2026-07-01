package com.jira.plan.controller;

import com.jira.plan.dto.InitiativeRequest;
import com.jira.plan.dto.InitiativeResponse;
import com.jira.plan.service.InitiativeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/initiatives")
@RequiredArgsConstructor
public class InitiativeController {

    private final InitiativeService initiativeService;

    @GetMapping
    public ResponseEntity<List<InitiativeResponse>> getAllInitiatives() {
        return ResponseEntity.ok(initiativeService.getAllInitiatives());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InitiativeResponse> getInitiative(@PathVariable UUID id) {
        return ResponseEntity.ok(initiativeService.getInitiative(id));
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<InitiativeResponse>> getInitiativesByProgram(@PathVariable UUID programId) {
        return ResponseEntity.ok(initiativeService.getInitiativesByProgram(programId));
    }

    @PostMapping
    public ResponseEntity<InitiativeResponse> createInitiative(
            @Valid @RequestBody InitiativeRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        InitiativeResponse response = initiativeService.createInitiative(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InitiativeResponse> updateInitiative(
            @PathVariable UUID id,
            @Valid @RequestBody InitiativeRequest request) {
        return ResponseEntity.ok(initiativeService.updateInitiative(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInitiative(@PathVariable UUID id) {
        initiativeService.deleteInitiative(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{initiativeId}/epics")
    public ResponseEntity<InitiativeResponse> addEpicToInitiative(
            @PathVariable UUID initiativeId,
            @RequestParam UUID epicId,
            @RequestParam String epicKey,
            @RequestParam String epicName) {
        return ResponseEntity.ok(initiativeService.addEpicToInitiative(initiativeId, epicId, epicKey, epicName));
    }

    @DeleteMapping("/{initiativeId}/epics/{epicId}")
    public ResponseEntity<Void> removeEpicFromInitiative(
            @PathVariable UUID initiativeId,
            @PathVariable UUID epicId) {
        initiativeService.removeEpicFromInitiative(initiativeId, epicId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{initiativeId}/plans")
    public ResponseEntity<InitiativeResponse> addPlanToInitiative(
            @PathVariable UUID initiativeId,
            @RequestParam UUID planId) {
        return ResponseEntity.ok(initiativeService.addPlanToInitiative(initiativeId, planId));
    }

    @DeleteMapping("/{initiativeId}/plans/{planId}")
    public ResponseEntity<Void> removePlanFromInitiative(
            @PathVariable UUID initiativeId,
            @PathVariable UUID planId) {
        initiativeService.removePlanFromInitiative(initiativeId, planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{initiativeId}/recalculate")
    public ResponseEntity<InitiativeResponse> recalculateProgress(@PathVariable UUID initiativeId) {
        initiativeService.recalculateInitiativeProgress(initiativeId);
        return ResponseEntity.ok(initiativeService.getInitiative(initiativeId));
    }
}
