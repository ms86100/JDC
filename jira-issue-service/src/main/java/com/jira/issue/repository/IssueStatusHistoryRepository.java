package com.jira.issue.repository;

import com.jira.issue.entity.IssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, UUID> {

    List<IssueStatusHistory> findByIssueIdOrderByChangedAtDesc(UUID issueId);
}
