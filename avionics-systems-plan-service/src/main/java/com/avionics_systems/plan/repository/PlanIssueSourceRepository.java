package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.PlanIssueSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanIssueSourceRepository extends JpaRepository<PlanIssueSource, UUID> {

    List<PlanIssueSource> findByPlanIdAndIsActiveTrue(UUID planId);

    List<PlanIssueSource> findByPlanId(UUID planId);

    Optional<PlanIssueSource> findByPlanIdAndSourceIdAndSourceType(
            UUID planId, UUID sourceId, PlanIssueSource.SourceType sourceType);

    boolean existsByPlanIdAndSourceIdAndSourceType(
            UUID planId, UUID sourceId, PlanIssueSource.SourceType sourceType);

    @Query("SELECT p FROM PlanIssueSource p WHERE p.sourceType = :type AND p.isActive = true")
    List<PlanIssueSource> findBySourceType(@Param("type") PlanIssueSource.SourceType type);

    @Query("SELECT p FROM PlanIssueSource p WHERE p.lastSyncAt < :threshold OR p.lastSyncAt IS NULL")
    List<PlanIssueSource> findStaleSources(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(p) FROM PlanIssueSource p WHERE p.plan.id = :planId AND p.isActive = true")
    long countActiveSourcesByPlanId(@Param("planId") UUID planId);

    @Query("SELECT SUM(p.issueCount) FROM PlanIssueSource p WHERE p.plan.id = :planId AND p.isActive = true")
    Integer sumIssueCountByPlanId(@Param("planId") UUID planId);

    void deleteByPlanId(UUID planId);
}