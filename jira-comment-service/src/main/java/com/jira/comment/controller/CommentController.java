package com.jira.comment.controller;

import com.jira.comment.dto.CommentResponse;
import com.jira.comment.dto.CreateCommentRequest;
import com.jira.comment.dto.UpdateCommentRequest;
import com.jira.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comments")
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("POST /comments - Creating comment for issue: {} by user: {}", request.getIssueId(), userId);
        CommentResponse response = commentService.createComment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/comments/issue/{issueId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByIssue(@PathVariable UUID issueId) {
        log.info("GET /comments/issue/{} - Fetching threaded comments", issueId);
        List<CommentResponse> response = commentService.getCommentsByIssueId(issueId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("PUT /comments/{} - Updating comment by user: {}", id, userId);
        CommentResponse response = commentService.updateComment(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("DELETE /comments/{} - Deleting comment by user: {}", id, userId);
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }
}