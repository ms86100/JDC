package com.jira.issue.repository;

import com.jira.issue.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueStatusRepository extends JpaRepository<IssueStatus, UUID> {

    Optional<IssueStatus> findByName(String name);

    Optional<IssueStatus> findFirstByCategoryOrderBySequenceAsc(String category);
}