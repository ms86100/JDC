package com.avionics_systems.plan.controller;

import com.avionics_systems.plan.dto.response.WarningResponse;
import com.avionics_systems.plan.service.WarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}/warnings")
@RequiredArgsConstructor
public class WarningController {

    private final WarningService warningService;

    @GetMapping
    public ResponseEntity<List<WarningResponse>> getWarnings(@PathVariable UUID planId) {
        return ResponseEntity.ok(warningService.getWarningsByPlanId(planId));
    }

    @PutMapping("/{warningId}/dismiss")
    public ResponseEntity<WarningResponse> dismissWarning(
            @PathVariable UUID planId,
            @PathVariable UUID warningId) {
        return ResponseEntity.ok(warningService.dismissWarning(planId, warningId));
    }
}
