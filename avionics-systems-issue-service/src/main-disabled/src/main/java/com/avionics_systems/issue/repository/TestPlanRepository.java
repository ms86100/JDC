package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.TestPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestPlanRepository extends JpaRepository<TestPlan, UUID> {

    List<TestPlan> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    @Query("SELECT tp FROM TestPlan tp WHERE tp.projectId = :projectId AND tp.status = :status")
    List<TestPlan> findByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") String status);

    @Query("SELECT tp FROM TestPlan tp WHERE tp.projectId = :projectId AND tp.startDate <= :date AND tp.endDate >= :date")
    List<TestPlan> findActivePlans(@Param("projectId") UUID projectId, @Param("date") LocalDate date);

    @Query("SELECT tp FROM TestPlan tp WHERE tp.projectId = :projectId AND tp.targetVersion = :version")
    List<TestPlan> findByProjectIdAndTargetVersion(@Param("projectId") UUID projectId, @Param("version") String version);

    @Query("SELECT tp FROM TestPlan tp WHERE tp.projectId = :projectId AND tp.environment = :env")
    List<TestPlan> findByProjectIdAndEnvironment(@Param("projectId") UUID projectId, @Param("env") String environment);
}