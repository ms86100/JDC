package com.avionics_systems.plan.controller;

import com.avionics_systems.plan.service.HierarchyRollupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}/hierarchy")
@RequiredArgsConstructor
public class HierarchyController {

    private final HierarchyRollupService hierarchyRollupService;

    @GetMapping("/metrics")
    public ResponseEntity<HierarchyRollupService.HierarchyMetrics> getHierarchyMetrics(@PathVariable UUID planId) {
        return ResponseEntity.ok(hierarchyRollupService.calculateRollup(planId));
    }

    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getHierarchyTree(@PathVariable UUID planId) {
        return ResponseEntity.ok(hierarchyRollupService.getHierarchyTree(planId));
    }
}
