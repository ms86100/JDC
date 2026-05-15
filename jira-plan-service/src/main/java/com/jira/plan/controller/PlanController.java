package com.jira.plan.controller;

import com.jira.plan.dto.request.CreatePlanRequest;
import com.jira.plan.dto.request.UpdatePlanRequest;
import com.jira.plan.dto.response.PlanResponse;
import com.jira.plan.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<PlanResponse>> getPlansByProgramId(@PathVariable UUID programId) {
        return ResponseEntity.ok(planService.getPlansByProgramId(programId));
    }

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        PlanResponse response = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(
            @PathVariable UUID id,
            @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<PlanResponse> updatePlanSettings(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> settings) {
        return ResponseEntity.ok(planService.updatePlanSettings(id, settings));
    }
}
