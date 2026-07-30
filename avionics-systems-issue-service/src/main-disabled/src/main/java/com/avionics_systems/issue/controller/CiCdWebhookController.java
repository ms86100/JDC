package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.CiCdIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CI/CD Webhook Controller - Jenkins, GitHub Actions, GitLab CI, Azure DevOps
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "CI/CD Webhooks", description = "CI/CD integration webhooks")
public class CiCdWebhookController {

    private final CiCdIntegrationService ciCdService;

    // ==================== GitHub Actions ====================

    @PostMapping("/github-actions")
    @Operation(summary = "GitHub Actions webhook endpoint")
    public ResponseEntity<WebhookResponse> handleGitHubActionsWebhook(
            @RequestParam UUID projectId,
            @RequestBody CiCdIntegrationService.GitHubWebhookPayload payload,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(ciCdService.handleGitHubActionsWebhook(projectId, payload, userId));
    }

    // ==================== Jenkins ====================

    @PostMapping("/jenkins")
    @Operation(summary = "Jenkins webhook endpoint")
    public ResponseEntity<WebhookResponse> handleJenkinsWebhook(
            @RequestParam UUID projectId,
            @RequestBody CiCdIntegrationService.JenkinsWebhookPayload payload,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(ciCdService.handleJenkinsWebhook(projectId, payload, userId));
    }

    // ==================== GitLab CI ====================

    @PostMapping("/gitlab")
    @Operation(summary = "GitLab CI webhook endpoint")
    public ResponseEntity<WebhookResponse> handleGitLabWebhook(
            @RequestParam UUID projectId,
            @RequestBody CiCdIntegrationService.GitLabWebhookPayload payload,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(ciCdService.handleGitLabWebhook(projectId, payload, userId));
    }

    // ==================== Azure DevOps ====================

    @PostMapping("/azure-devops")
    @Operation(summary = "Azure DevOps webhook endpoint")
    public ResponseEntity<WebhookResponse> handleAzureDevOpsWebhook(
            @RequestParam UUID projectId,
            @RequestBody CiCdIntegrationService.AzureDevOpsWebhookPayload payload,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(ciCdService.handleAzureDevOpsWebhook(projectId, payload, userId));
    }

    // ==================== CI Execution Trigger ====================

    @PostMapping("/trigger")
    @Operation(summary = "Trigger test execution from CI/CD")
    public ResponseEntity<TestExecutionResponse> triggerExecution(
            @RequestParam UUID projectId,
            @RequestBody CiExecutionRequest request,
            @RequestHeader(value = "X-User-Id") UUID userId) {
        return ResponseEntity.ok(ciCdService.triggerExecutionFromCi(projectId, request, userId));
    }

    @PutMapping("/execution/{executionId}")
    @Operation(summary = "Update execution from CI/CD")
    public ResponseEntity<Void> updateExecution(
            @PathVariable UUID executionId,
            @RequestBody CiExecutionUpdate update) {
        ciCdService.updateExecutionFromCi(executionId, update);
        return ResponseEntity.ok().build();
    }
}