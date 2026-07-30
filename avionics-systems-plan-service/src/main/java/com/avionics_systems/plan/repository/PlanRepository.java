package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByOwnerId(UUID ownerId);

    List<Plan> findByIsActiveTrue();

    @Query("SELECT p FROM Plan p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<Plan> findAllActiveOrderByCreatedAtDesc();

    @Query("SELECT p FROM Plan p LEFT JOIN FETCH p.teams LEFT JOIN FETCH p.releases WHERE p.id = :id")
    Plan findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT COUNT(pi) FROM PlanItem pi WHERE pi.plan.id = :planId")
    long countItemsByPlanId(@Param("planId") UUID planId);

    @Query("SELECT COUNT(pt) FROM PlanTeam pt WHERE pt.plan.id = :planId AND pt.isActive = true")
    long countTeamsByPlanId(@Param("planId") UUID planId);

    @Query("SELECT COUNT(pr) FROM PlanRelease pr WHERE pr.plan.id = :planId")
    long countReleasesByPlanId(@Param("planId") UUID planId);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT p FROM Plan p JOIN p.programs pr WHERE pr.id = :programId")
    List<Plan> findByProgramId(@Param("programId") UUID programId);
}