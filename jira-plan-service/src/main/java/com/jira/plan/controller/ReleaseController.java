package com.jira.plan.controller;

import com.jira.plan.dto.request.CreateReleaseRequest;
import com.jira.plan.dto.response.ReleaseResponse;
import com.jira.plan.service.ReleaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping
    public ResponseEntity<List<ReleaseResponse>> getReleases(@PathVariable UUID planId) {
        return ResponseEntity.ok(releaseService.getReleasesByPlanId(planId));
    }

    @GetMapping("/{releaseId}")
    public ResponseEntity<ReleaseResponse> getReleaseById(
            @PathVariable UUID planId,
            @PathVariable UUID releaseId) {
        return ResponseEntity.ok(releaseService.getReleaseById(planId, releaseId));
    }

    @PostMapping
    public ResponseEntity<ReleaseResponse> createRelease(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateReleaseRequest request) {
        ReleaseResponse response = releaseService.createRelease(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{releaseId}")
    public ResponseEntity<ReleaseResponse> updateRelease(
            @PathVariable UUID planId,
            @PathVariable UUID releaseId,
            @RequestBody CreateReleaseRequest request) {
        return ResponseEntity.ok(releaseService.updateRelease(planId, releaseId, request));
    }

    @PostMapping("/{releaseId}/approve")
    public ResponseEntity<ReleaseResponse> approveRelease(
            @PathVariable UUID planId,
            @PathVariable UUID releaseId,
            @RequestParam UUID approvedBy) {
        return ResponseEntity.ok(releaseService.approveRelease(planId, releaseId, approvedBy));
    }

    @PostMapping("/{releaseId}/release")
    public ResponseEntity<ReleaseResponse> releaseVersion(
            @PathVariable UUID planId,
            @PathVariable UUID releaseId) {
        return ResponseEntity.ok(releaseService.releaseVersion(planId, releaseId));
    }

    @DeleteMapping("/{releaseId}")
    public ResponseEntity<Void> deleteRelease(
            @PathVariable UUID planId,
            @PathVariable UUID releaseId) {
        releaseService.deleteRelease(planId, releaseId);
        return ResponseEntity.noContent().build();
    }
}
