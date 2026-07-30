package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.DevInfoBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DevInfoBranchRepository extends JpaRepository<DevInfoBranch, UUID> {
    List<DevInfoBranch> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
    long countByIssueId(UUID issueId);
}
