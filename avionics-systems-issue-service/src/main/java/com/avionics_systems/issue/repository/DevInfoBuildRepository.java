package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.DevInfoBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DevInfoBuildRepository extends JpaRepository<DevInfoBuild, UUID> {
    List<DevInfoBuild> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
    long countByIssueId(UUID issueId);
}
