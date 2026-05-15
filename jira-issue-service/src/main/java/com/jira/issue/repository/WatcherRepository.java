package com.jira.issue.repository;

import com.jira.issue.entity.Watcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatcherRepository extends JpaRepository<Watcher, UUID> {
    List<Watcher> findByIssueId(UUID issueId);
    List<Watcher> findByUserId(UUID userId);
    Optional<Watcher> findByIssueIdAndUserId(UUID issueId, UUID userId);
    boolean existsByIssueIdAndUserId(UUID issueId, UUID userId);
    void deleteByIssueIdAndUserId(UUID issueId, UUID userId);
    long countByIssueId(UUID issueId);
}