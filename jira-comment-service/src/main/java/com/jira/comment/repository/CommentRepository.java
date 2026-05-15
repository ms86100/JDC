package com.jira.comment.repository;

import com.jira.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("SELECT c FROM jira_comment.comments c WHERE c.issueId = :issueId AND c.deleted = false ORDER BY c.createdAt ASC")
    List<Comment> findByIssueIdAndDeletedFalse(@Param("issueId") UUID issueId);

    @Query("SELECT c FROM jira_comment.comments c WHERE c.issueId = :issueId AND c.parentComment IS NULL AND c.deleted = false ORDER BY c.createdAt ASC")
    List<Comment> findRootCommentsByIssueId(@Param("issueId") UUID issueId);

    boolean existsByIdAndDeletedFalse(UUID id);
}