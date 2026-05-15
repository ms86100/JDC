package com.jira.plan.repository;

import com.jira.plan.entity.PlanRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanReleaseRepository extends JpaRepository<PlanRelease, UUID> {

    List<PlanRelease> findByPlanIdOrderByReleaseDateAsc(UUID planId);

    List<PlanRelease> findByPlanIdAndStatus(UUID planId, String status);

    List<PlanRelease> findByPlanIdAndStatusOrderByReleaseDateAsc(UUID planId, String status);

    List<PlanRelease> findByPlanIdOrderByReleaseDateDesc(UUID planId);
}