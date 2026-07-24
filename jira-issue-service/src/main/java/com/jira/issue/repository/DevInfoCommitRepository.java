package com.jira.issue.repository;

import com.jira.issue.entity.DevInfoCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DevInfoCommitRepository extends JpaRepository<DevInfoCommit, UUID> {
    List<DevInfoCommit> findByIssueIdOrderByCommittedAtDesc(UUID issueId);
    long countByIssueId(UUID issueId);
    boolean existsByIssueIdAndCommitHash(UUID issueId, String commitHash);
}
