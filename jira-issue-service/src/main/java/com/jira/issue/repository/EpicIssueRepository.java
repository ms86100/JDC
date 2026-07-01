package com.jira.issue.repository;

import com.jira.issue.entity.EpicIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpicIssueRepository extends JpaRepository<EpicIssue, String> {

    List<EpicIssue> findByEpicId(String epicId);

    List<EpicIssue> findByIssueId(String issueId);

    Optional<EpicIssue> findByEpicIdAndIssueId(String epicId, String issueId);

    boolean existsByEpicIdAndIssueId(String epicId, String issueId);

    @Query("SELECT ei.issueId FROM EpicIssue ei WHERE ei.epicId = :epicId")
    List<String> findIssueIdsByEpicId(@Param("epicId") String epicId);

    @Query("SELECT ei.epicId FROM EpicIssue ei WHERE ei.issueId = :issueId")
    List<String> findEpicIdsByIssueId(@Param("issueId") String issueId);

    void deleteByEpicIdAndIssueId(String epicId, String issueId);
}