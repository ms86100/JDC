package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.DevInfoPullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DevInfoPullRequestRepository extends JpaRepository<DevInfoPullRequest, UUID> {
    List<DevInfoPullRequest> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
    long countByIssueId(UUID issueId);
    List<DevInfoPullRequest> findByIssueIdAndStatus(UUID issueId, String status);
}
