package com.jira.plan.repository;

import com.jira.plan.entity.PlanWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanWarningRepository extends JpaRepository<PlanWarning, UUID> {

    @Query("SELECT pw FROM PlanWarning pw WHERE pw.plan.id = :planId AND pw.isActive = true ORDER BY pw.createdAt DESC")
    List<PlanWarning> findActiveByPlanId(@Param("planId") UUID planId);

    List<PlanWarning> findByPlanIdAndIsActiveTrueOrderByCreatedAtDesc(UUID planId);

    List<PlanWarning> findByPlanIdAndIsActiveFalseOrderByDismissedAtDesc(UUID planId);

    @Query("SELECT pw FROM PlanWarning pw WHERE pw.plan.id = :planId AND pw.warningType = :warningType AND pw.isActive = true")
    List<PlanWarning> findActiveByPlanIdAndWarningType(@Param("planId") UUID planId, @Param("warningType") String warningType);

    @Query("SELECT COUNT(pw) FROM PlanWarning pw WHERE pw.plan.id = :planId AND pw.isActive = true")
    long countActiveByPlanId(@Param("planId") UUID planId);

    List<PlanWarning> findByPlanIdAndIsActiveTrue(UUID planId);
}