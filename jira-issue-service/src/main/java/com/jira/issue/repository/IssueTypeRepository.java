package com.jira.issue.repository;

import com.jira.issue.entity.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueTypeRepository extends JpaRepository<IssueType, UUID> {

    Optional<IssueType> findByName(String name);

    Optional<IssueType> findByNameIgnoreCase(String name);

    Optional<IssueType> findByIssueTypeKey(String issueTypeKey);

    boolean existsByIssueTypeKey(String issueTypeKey);
}