package com.jira.issue.repository;

import com.jira.issue.entity.IssueTransitionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueTransitionHistoryRepository extends JpaRepository<IssueTransitionHistory, UUID> {

    List<IssueTransitionHistory> findByIssueIdOrderByExecutedAtDesc(UUID issueId);
}
