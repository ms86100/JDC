package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.PlanTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanTeamRepository extends JpaRepository<PlanTeam, UUID> {

    List<PlanTeam> findByPlanIdAndIsActiveTrue(UUID planId);

    @Query("SELECT pt FROM PlanTeam pt LEFT JOIN FETCH pt.members WHERE pt.id = :id")
    PlanTeam findByIdWithMembers(@Param("id") UUID id);

    @Query("SELECT pt FROM PlanTeam pt LEFT JOIN FETCH pt.members WHERE pt.plan.id = :planId AND pt.isActive = true")
    List<PlanTeam> findByPlanIdWithMembers(@Param("planId") UUID planId);

    boolean existsByPlanIdAndNameIgnoreCaseAndIdNot(UUID planId, String name, UUID id);

    boolean existsByPlanIdAndNameIgnoreCase(UUID planId, String name);
}