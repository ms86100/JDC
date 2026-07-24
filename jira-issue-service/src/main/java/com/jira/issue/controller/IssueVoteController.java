package com.jira.issue.controller;

import com.jira.issue.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for voting on issues.
 * Endpoints match frontend expectations at /api/issues/{id}/vote
 */
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Issue Voting", description = "Vote for/against issues")
public class IssueVoteController {

    private final VoteService voteService;

    @PostMapping("/{issueId}/vote")
    @Operation(summary = "Vote for an issue", description = "Add your vote to an issue")
    public ResponseEntity<Void> vote(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("User {} voting for issue {}", userId, issueId);
        if (userId != null) {
            voteService.addVote(issueId, userId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{issueId}/vote")
    @Operation(summary = "Remove vote from issue", description = "Remove your vote from an issue")
    public ResponseEntity<Void> unvote(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("User {} removing vote from issue {}", userId, issueId);
        if (userId != null) {
            voteService.removeVote(issueId, userId);
        }
        return ResponseEntity.noContent().build();
    }
}