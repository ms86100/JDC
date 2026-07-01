package com.jira.plan.controller;

import com.jira.plan.service.CriticalPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/critical-path")
@RequiredArgsConstructor
public class CriticalPathController {

    private final CriticalPathService criticalPathService;

    @GetMapping("/calculate/{planId}")
    public ResponseEntity<CriticalPathService.CriticalPathResult> calculateCriticalPath(@PathVariable UUID planId) {
        return ResponseEntity.ok(criticalPathService.calculateCriticalPath(planId));
    }

    @PostMapping("/analyze-risk/{planId}")
    public ResponseEntity<CriticalPathService.RiskAnalysis> analyzeRisks(
            @PathVariable UUID planId,
            @RequestParam UUID changeItemId,
            @RequestParam(defaultValue = "0") int changeDays) {
        return ResponseEntity.ok(criticalPathService.analyzeRisks(planId, changeItemId, changeDays));
    }
}
