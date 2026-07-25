package com.jira.comment.service;

import com.jira.comment.dto.CommentResponse;
import com.jira.comment.dto.CreateCommentRequest;
import com.jira.comment.dto.UpdateCommentRequest;
import com.jira.comment.entity.Comment;
import com.jira.comment.exception.OptimisticLockException;
import com.jira.comment.exception.ResourceNotFoundException;
import com.jira.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final MessageSource messageSource;

    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, UUID userId) {
        log.info("Creating comment for issue: {} by user: {}", request.getIssueId(), userId);

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageSource.getMessage("comment.parent.not.found",
                                    new Object[]{request.getParentCommentId()}, Locale.ENGLISH)));

            if (parentComment.getDeleted()) {
                throw new ResourceNotFoundException(
                        messageSource.getMessage("comment.parent.deleted", null, Locale.ENGLISH));
            }
        }

        Comment comment = Comment.builder()
                .issueId(request.getIssueId())
                .userId(userId)
                .parentComment(parentComment)
                .content(request.getContent())
                .deleted(false)
                .internal(request.getInternal() != null ? request.getInternal() : false)
                .build();

        comment = commentRepository.save(comment);
        log.info("Created comment with id: {}", comment.getId());

        return mapToResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByIssueId(UUID issueId) {
        log.debug("Fetching comments for issue: {}", issueId);

        List<Comment> allComments = commentRepository.findByIssueIdAndDeletedFalse(issueId);

        // Build the tree structure
        Map<UUID, CommentResponse> commentMap = new HashMap<>();
        List<CommentResponse> rootComments = new ArrayList<>();

        // First pass: create all CommentResponse objects
        for (Comment comment : allComments) {
            CommentResponse response = mapToResponse(comment);
            commentMap.put(comment.getId(), response);
        }

        // Second pass: build the tree
        for (Comment comment : allComments) {
            CommentResponse response = commentMap.get(comment.getId());
            if (comment.getParentComment() == null) {
                rootComments.add(response);
            } else {
                CommentResponse parentResponse = commentMap.get(comment.getParentComment().getId());
                if (parentResponse != null) {
                    parentResponse.getReplies().add(response);
                }
            }
        }

        return rootComments;
    }

    @Transactional
    public CommentResponse updateComment(UUID commentId, UpdateCommentRequest request, UUID userId) {
        log.info("Updating comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("comment.not.found",
                                new Object[]{commentId}, Locale.ENGLISH)));

        if (comment.getDeleted()) {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("comment.deleted", null, Locale.ENGLISH));
        }

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("comment.update.unauthorized", null, Locale.ENGLISH));
        }

        // Optimistic locking: check version if provided
        if (request.getVersion() != null && !request.getVersion().equals(comment.getVersion())) {
            throw new OptimisticLockException(
                    messageSource.getMessage("comment.optimistic.lock",
                            new Object[]{comment.getVersion(), request.getVersion()}, Locale.ENGLISH));
        }

        comment.setContent(request.getContent());
        if (request.getInternal() != null) {
            comment.setInternal(request.getInternal());
        }
        comment = commentRepository.save(comment);
        log.info("Updated comment: {}", commentId);

        return mapToResponse(comment);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        log.info("Deleting (soft) comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("comment.not.found",
                                new Object[]{commentId}, Locale.ENGLISH)));

        if (comment.getDeleted()) {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("comment.already.deleted", null, Locale.ENGLISH));
        }

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("comment.delete.unauthorized", null, Locale.ENGLISH));
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
        log.info("Soft deleted comment: {}", commentId);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByIssueIdPaginated(UUID issueId, Pageable pageable) {
        log.debug("Fetching paginated comments for issue: {}", issueId);
        Page<Comment> commentPage = commentRepository.findByIssueIdAndDeletedFalse(issueId, pageable);
        return commentPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByIssueIdPaginated(UUID issueId, Pageable pageable, Boolean internal) {
        log.debug("Fetching paginated comments for issue: {} with internal filter: {}", issueId, internal);
        if (internal != null) {
            Page<Comment> commentPage = commentRepository.findRootCommentsByIssueId(issueId, pageable);
            // Filter by internal flag in memory since no specific query exists
            List<Comment> filtered = commentPage.getContent().stream()
                    .filter(c -> internal.equals(c.getInternal()))
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(
                    filtered.stream().map(this::mapToResponse).collect(Collectors.toList()),
                    pageable,
                    commentPage.getTotalElements());
        }
        return getCommentsByIssueIdPaginated(issueId, pageable);
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .issueId(comment.getIssueId())
                .userId(comment.getUserId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.getContent())
                .version(comment.getVersion())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .internal(comment.getInternal())
                .replies(new ArrayList<>())
                .build();
    }
}