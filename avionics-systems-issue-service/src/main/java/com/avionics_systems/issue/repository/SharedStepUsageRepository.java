package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.SharedStepUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SharedStepUsageRepository extends JpaRepository<SharedStepUsage, UUID> {

    List<SharedStepUsage> findBySharedStepId(UUID sharedStepId);

    List<SharedStepUsage> findByTestIssueId(UUID testIssueId);

    boolean existsBySharedStepIdAndTestIssueId(UUID sharedStepId, UUID testIssueId);
}