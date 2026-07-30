package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.ExclusionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExclusionRuleRepository extends JpaRepository<ExclusionRule, UUID> {

    List<ExclusionRule> findByPlanIdAndIsActiveTrue(UUID planId);

    List<ExclusionRule> findByPlanId(UUID planId);

    @Query("SELECT e FROM ExclusionRule e WHERE e.plan.id = :planId AND e.isActive = true ORDER BY e.createdAt ASC")
    List<ExclusionRule> findActiveRulesByPlanId(@Param("planId") UUID planId);

    @Query("SELECT e FROM ExclusionRule e WHERE e.fieldName = :fieldName AND e.isActive = true")
    List<ExclusionRule> findByFieldName(@Param("fieldName") String fieldName);

    void deleteByPlanId(UUID planId);

    long countByPlanIdAndIsActiveTrue(UUID planId);
}