package com.jira.issue.repository;

import com.jira.issue.event.IssueEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueEventOutboxRepository extends JpaRepository<IssueEventOutbox, UUID> {
    List<IssueEventOutbox> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
