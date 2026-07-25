package com.jira.issue.repository;

import com.jira.issue.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId ORDER BY i.createdAt DESC")
    Page<Issue> findByProjectId(@Param("projectId") UUID projectId, Pageable pageable);

    Optional<Issue> findByIssueKey(String issueKey);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(issue_key, LENGTH(:projectKey) + 2) AS integer)) " +
           "FROM jira_issue.issues WHERE issue_key ~ CONCAT(:projectKey, '-[0-9]+$')",
           nativeQuery = true)
    Optional<Integer> findMaxIssueNumberByProjectKey(@Param("projectKey") String projectKey);

    /**
     * Locking version for concurrent key generation.
     * Uses pessimistic locking to prevent race conditions in issue key generation.
     */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(issue_key, LENGTH(:projectKey) + 2) AS integer)) " +
           "FROM jira_issue.issues WHERE issue_key ~ CONCAT(:projectKey, '-[0-9]+$')",
           nativeQuery = true)
    Optional<Integer> findMaxIssueNumberByProjectKeyForUpdate(@Param("projectKey") String projectKey);

    List<Issue> findByParentIssueId(UUID parentIssueId);

    @Query("SELECT i FROM Issue i WHERE i.epicId = :epicId ORDER BY i.createdAt ASC")
    List<Issue> findByEpicId(@Param("epicId") UUID epicId);

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId AND i.status.id = :statusId")
    List<Issue> findByProjectIdAndStatusId(@Param("projectId") UUID projectId, @Param("statusId") UUID statusId);

    @Query("SELECT i FROM Issue i WHERE i.assigneeId = :assigneeId ORDER BY i.createdAt DESC")
    Page<Issue> findByAssigneeId(@Param("assigneeId") UUID assigneeId, Pageable pageable);

    @Query("SELECT i FROM Issue i WHERE i.reporterId = :reporterId ORDER BY i.createdAt DESC")
    Page<Issue> findByReporterId(@Param("reporterId") UUID reporterId, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.projectId = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.issueType.id = :issueTypeId")
    long countByIssueTypeId(@Param("issueTypeId") UUID issueTypeId);

    @Query("SELECT i FROM Issue i JOIN i.status s WHERE i.projectId = :projectId AND s.category = :category")
    List<Issue> findByProjectIdAndStatusCategory(@Param("projectId") UUID projectId, @Param("category") String category);

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId AND i.issueType.name = :typeName")
    List<Issue> findByProjectIdAndIssueTypeName(@Param("projectId") UUID projectId, @Param("typeName") String typeName);

    @Query("SELECT i FROM Issue i WHERE i.projectId = :projectId ORDER BY i.rank ASC NULLS LAST, i.updatedAt DESC")
    List<Issue> findByProjectIdForRanking(@Param("projectId") UUID projectId);
}