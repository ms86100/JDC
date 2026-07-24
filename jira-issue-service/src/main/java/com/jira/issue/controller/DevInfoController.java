package com.jira.issue.controller;

import com.jira.issue.service.DevInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DevInfoController {

    private final DevInfoService devInfoService;

    @GetMapping("/api/issues/{issueId}/dev-info")
    public ResponseEntity<Map<String, Object>> getDevInfo(@PathVariable UUID issueId) {
        return ResponseEntity.ok(devInfoService.getDevInfoForIssue(issueId));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/api/dev-info/commits")
    public ResponseEntity<?> processCommits(@RequestBody Map<String, Object> payload) {
        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
        if (commits == null || commits.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No commits provided"));
        }
        var saved = devInfoService.processCommits(commits);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("linked", saved.size()));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/api/dev-info/pull-requests")
    public ResponseEntity<?> processPullRequests(@RequestBody Map<String, Object> payload) {
        List<Map<String, Object>> prs = (List<Map<String, Object>>) payload.get("pullRequests");
        if (prs == null || prs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No pull requests provided"));
        }
        var saved = devInfoService.processPullRequests(prs);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("linked", saved.size()));
    }

    @GetMapping("/api/issues/{issueId}/create-branch-url")
    public ResponseEntity<Map<String, String>> getCreateBranchUrl(@PathVariable UUID issueId) {
        String branchName = devInfoService.generateBranchName(issueId);
        if (branchName == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("suggestedBranchName", branchName));
    }

    @PostMapping("/api/issues/{issueId}/branches")
    public ResponseEntity<?> recordBranch(@PathVariable UUID issueId,
                                           @RequestBody Map<String, String> payload) {
        var branch = devInfoService.createBranchRecord(issueId,
                payload.get("branchName"),
                payload.get("repository"),
                payload.get("url"));
        return ResponseEntity.status(HttpStatus.CREATED).body(branch);
    }

    // === SCM Webhook Endpoints ===

    private final com.jira.issue.service.WebhookParserService webhookParserService;

    @PostMapping("/api/dev-info/webhooks/github")
    public ResponseEntity<Map<String, Object>> handleGitHubWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {
        int linked = 0;
        if ("push".equals(eventType)) {
            linked = webhookParserService.parseGitHubPushEvent(payload);
        } else if ("pull_request".equals(eventType)) {
            linked = webhookParserService.parseGitHubPullRequestEvent(payload);
        }
        return ResponseEntity.ok(Map.of("status", "processed", "event", eventType != null ? eventType : "unknown", "linked", linked));
    }

    @PostMapping("/api/dev-info/webhooks/gitlab")
    public ResponseEntity<Map<String, Object>> handleGitLabWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Gitlab-Event", required = false) String eventType) {
        int linked = 0;
        if ("Push Hook".equals(eventType)) {
            linked = webhookParserService.parseGitLabPushEvent(payload);
        } else if ("Merge Request Hook".equals(eventType)) {
            linked = webhookParserService.parseGitLabMergeRequestEvent(payload);
        }
        return ResponseEntity.ok(Map.of("status", "processed", "event", eventType != null ? eventType : "unknown", "linked", linked));
    }

    @PostMapping("/api/dev-info/webhooks/bitbucket")
    public ResponseEntity<Map<String, Object>> handleBitbucketWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Event-Key", required = false) String eventKey) {
        int linked = 0;
        if (eventKey != null && eventKey.startsWith("repo:push")) {
            linked = webhookParserService.parseBitbucketPushEvent(payload);
        } else if (eventKey != null && eventKey.startsWith("pullrequest:")) {
            linked = webhookParserService.parseBitbucketPullRequestEvent(payload);
        }
        return ResponseEntity.ok(Map.of("status", "processed", "event", eventKey != null ? eventKey : "unknown", "linked", linked));
    }
}
