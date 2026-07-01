package com.jira.issue.controller;

import com.jira.issue.dto.VoteResponse;
import com.jira.issue.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing issue votes
 */
@RestController
@RequestMapping("/api/issues/{issueId}/votes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Issue Votes", description = "Vote management for issues")
@CrossOrigin(origins = "*")
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    @Operation(summary = "Add vote to issue", description = "Vote for an issue")
    public ResponseEntity<VoteResponse> addVote(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        log.info("Adding vote for issue {} by user {}", issueId, userId);
        VoteResponse response = voteService.addVote(issueId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    @Operation(summary = "Remove vote from issue", description = "Remove your vote from an issue")
    public ResponseEntity<Void> removeVote(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        log.info("Removing vote for issue {} by user {}", issueId, userId);
        voteService.removeVote(issueId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get votes for issue", description = "Get all votes for an issue")
    public ResponseEntity<List<VoteResponse>> getVotes(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        log.info("Getting votes for issue: {}", issueId);
        List<VoteResponse> votes = voteService.getVotesByIssue(issueId);
        return ResponseEntity.ok(votes);
    }

    @GetMapping("/count")
    @Operation(summary = "Get vote count", description = "Get the number of votes for an issue")
    public ResponseEntity<Long> getVoteCount(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        log.info("Getting vote count for issue: {}", issueId);
        long count = voteService.getVoteCount(issueId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/check")
    @Operation(summary = "Check if user voted", description = "Check if the current user has voted")
    public ResponseEntity<Boolean> hasVoted(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        log.info("Checking if user {} voted for issue {}", userId, issueId);
        boolean voted = voteService.hasVoted(issueId, userId);
        return ResponseEntity.ok(voted);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get votes by user", description = "Get all votes by a specific user")
    public ResponseEntity<List<VoteResponse>> getVotesByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        log.info("Getting votes by user: {}", userId);
        List<VoteResponse> votes = voteService.getVotesByUser(userId);
        return ResponseEntity.ok(votes);
    }
}