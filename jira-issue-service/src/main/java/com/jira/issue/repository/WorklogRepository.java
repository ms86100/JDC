package com.jira.issue.repository;

import com.jira.issue.entity.Worklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorklogRepository extends JpaRepository<Worklog, UUID> {
    List<Worklog> findByIssueIdOrderByStartedAtDesc(UUID issueId);
    List<Worklog> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
    long countByIssueId(UUID issueId);

    @Query("SELECT SUM(w.timeSpentSeconds) FROM Worklog w WHERE w.issueId = :issueId")
    Long getTotalTimeSpent(@Param("issueId") UUID issueId);

    List<Worklog> findByStartedAtBetween(LocalDateTime start, LocalDateTime end);
}