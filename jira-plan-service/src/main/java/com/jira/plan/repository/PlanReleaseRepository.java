package com.jira.plan.repository;

import com.jira.plan.entity.PlanRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanReleaseRepository extends JpaRepository<PlanRelease, UUID> {

    // Default queries filter by is_active = true
    List<PlanRelease> findByPlanIdOrderByReleaseDateAsc(UUID planId);

    List<PlanRelease> findByPlanIdAndStatus(UUID planId, String status);

    List<PlanRelease> findByPlanIdAndStatusOrderByReleaseDateAsc(UUID planId, String status);

    List<PlanRelease> findByPlanIdOrderByReleaseDateDesc(UUID planId);

    // Active releases only (default behavior)
    List<PlanRelease> findByPlanIdAndIsActiveTrueOrderByReleaseDateDesc(UUID planId);

    // Include deleted (for admin/audit purposes)
    @Query("SELECT r FROM PlanRelease r WHERE r.planId = :planId ORDER BY r.releaseDate DESC")
    List<PlanRelease> findAllByPlanId(@Param("planId") UUID planId);

    // Find active release by ID
    Optional<PlanRelease> findByIdAndIsActiveTrue(UUID id);

    // Find any release by ID (including deleted)
    Optional<PlanRelease> findById(UUID id);
}