package com.avionics_systems.plan.controller;

import com.avionics_systems.plan.dto.request.CreateDependencyRequest;
import com.avionics_systems.plan.dto.response.DependencyResponse;
import com.avionics_systems.plan.service.DependencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}/dependencies")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    @GetMapping
    public ResponseEntity<List<DependencyResponse>> getDependencies(@PathVariable UUID planId) {
        return ResponseEntity.ok(dependencyService.getDependenciesByPlanId(planId));
    }

    @PostMapping
    public ResponseEntity<DependencyResponse> createDependency(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateDependencyRequest request) {
        DependencyResponse response = dependencyService.createDependency(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<Void> deleteDependency(
            @PathVariable UUID planId,
            @PathVariable UUID dependencyId) {
        dependencyService.deleteDependency(planId, dependencyId);
        return ResponseEntity.noContent().build();
    }
}
