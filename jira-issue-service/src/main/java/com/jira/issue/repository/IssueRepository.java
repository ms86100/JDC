package com.jira.issue.repository;

import com.jira.issue.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId ORDER BY i.createdAt DESC")
    Page<Issue> findByProjectId(@Param("projectId") UUID projectId, Pageable pageable);

    Optional<Issue> findByIssueKey(String issueKey);

    @Query("SELECT MAX(CAST(SUBSTRING(i.issueKey, LENGTH(:projectKey) + 2) AS integer)) " +
           "FROM Issue i WHERE i.issueKey LIKE :projectKey || '-%'")
    Optional<Integer> findMaxIssueNumberByProjectKey(@Param("projectKey") String projectKey);
}