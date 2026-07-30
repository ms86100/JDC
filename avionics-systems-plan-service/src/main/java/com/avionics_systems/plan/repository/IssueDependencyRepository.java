package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.IssueDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueDependencyRepository extends JpaRepository<IssueDependency, UUID> {

    List<IssueDependency> findByPlanId(UUID planId);

    @Query("SELECT d FROM IssueDependency d WHERE d.plan.id = :planId AND d.blockingIssueId = :issueId")
    List<IssueDependency> findByPlanIdAndBlockingIssueId(@Param("planId") UUID planId, @Param("issueId") UUID issueId);

    @Query("SELECT d FROM IssueDependency d WHERE d.plan.id = :planId AND d.blockedIssueId = :issueId")
    List<IssueDependency> findByPlanIdAndBlockedIssueId(@Param("planId") UUID planId, @Param("issueId") UUID issueId);

    Optional<IssueDependency> findByPlanIdAndBlockingIssueIdAndBlockedIssueId(UUID planId, UUID blockingIssueId, UUID blockedIssueId);

    boolean existsByPlanIdAndBlockingIssueIdAndBlockedIssueId(UUID planId, UUID blockingIssueId, UUID blockedIssueId);

    boolean existsByBlockingIssueIdAndBlockedIssueId(UUID blockingIssueId, UUID blockedIssueId);
}