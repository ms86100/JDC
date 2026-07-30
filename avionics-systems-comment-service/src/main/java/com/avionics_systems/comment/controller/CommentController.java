package com.avionics_systems.comment.controller;

import com.avionics_systems.comment.dto.CommentResponse;
import com.avionics_systems.comment.dto.CreateCommentRequest;
import com.avionics_systems.comment.dto.UpdateCommentRequest;
import com.avionics_systems.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader(value = "X-User-Id") UUID userId) {
        UUID actor = userId;
        log.info("POST /comments - Creating comment for issue: {} by user: {}", request.getIssueId(), actor);
        CommentResponse response = commentService.createComment(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByIssue(@PathVariable UUID issueId) {
        log.info("GET /comments/issue/{} - Fetching threaded comments", issueId);
        List<CommentResponse> response = commentService.getCommentsByIssueId(issueId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issue/{issueId}/paginated")
    public ResponseEntity<Page<CommentResponse>> getCommentsByIssuePaginated(
            @PathVariable UUID issueId,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Boolean internal) {
        log.info("GET /comments/issue/{}/paginated - Fetching paginated comments", issueId);
        Page<CommentResponse> response = commentService.getCommentsByIssueIdPaginated(issueId, pageable, internal);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("PUT /comments/{} - Updating comment by user: {}", id, userId);
        CommentResponse response = commentService.updateComment(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("DELETE /comments/{} - Deleting comment by user: {}", id, userId);
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }
}