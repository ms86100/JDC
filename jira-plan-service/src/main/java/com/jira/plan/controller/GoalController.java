package com.jira.plan.controller;

import com.jira.plan.dto.CreateGoalRequest;
import com.jira.plan.dto.GoalResponse;
import com.jira.plan.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getGoals(@PathVariable UUID planId) {
        return ResponseEntity.ok(goalService.getGoalsByPlanId(planId));
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<GoalResponse> getGoalById(
            @PathVariable UUID planId,
            @PathVariable UUID goalId) {
        return ResponseEntity.ok(goalService.getGoalById(planId, goalId));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateGoalRequest request) {
        GoalResponse response = goalService.createGoal(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable UUID planId,
            @PathVariable UUID goalId,
            @RequestBody CreateGoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(planId, goalId, request));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID planId,
            @PathVariable UUID goalId) {
        goalService.deleteGoal(planId, goalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{goalId}/children")
    public ResponseEntity<List<GoalResponse>> getGoalChildren(
            @PathVariable UUID planId,
            @PathVariable UUID goalId) {
        return ResponseEntity.ok(goalService.getGoalChildren(planId, goalId));
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<List<GoalResponse>> getGoalHierarchy(@PathVariable UUID planId) {
        return ResponseEntity.ok(goalService.getGoalHierarchy(planId));
    }

    @PostMapping("/{goalId}/calculate-progress")
    public ResponseEntity<GoalResponse> calculateProgress(
            @PathVariable UUID planId,
            @PathVariable UUID goalId) {
        return ResponseEntity.ok(goalService.calculateProgress(planId, goalId));
    }
}
