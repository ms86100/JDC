package com.jira.issue.repository;

import com.jira.issue.entity.SharedStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedStepRepository extends JpaRepository<SharedStep, UUID> {

    List<SharedStep> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<SharedStep> findByProjectIdAndName(UUID projectId, String name);

    @Query("SELECT ss FROM SharedStep ss WHERE ss.projectId = :projectId AND ss.stepType = :stepType")
    List<SharedStep> findByProjectIdAndStepType(@Param("projectId") UUID projectId, @Param("stepType") String stepType);

    @Query("SELECT ss FROM SharedStep ss WHERE ss.usageCount > 0 ORDER BY ss.usageCount DESC")
    List<SharedStep> findMostUsed();

    @Query("SELECT ss FROM SharedStep ss WHERE ss.projectId = :projectId AND ss.name LIKE %:query%")
    List<SharedStep> searchByName(@Param("projectId") UUID projectId, @Param("query") String query);
}