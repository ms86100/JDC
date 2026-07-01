package com.jira.plan.repository;

import com.jira.plan.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {

    List<Program> findByOwnerId(UUID ownerId);

    List<Program> findByIsActiveTrue();

    @Query("SELECT p FROM Program p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<Program> findAllActiveOrderByCreatedAtDesc();

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.plans WHERE p.id = :id")
    Program findByIdWithPlans(@Param("id") UUID id);

    @Query("SELECT COUNT(pp) FROM Plan pp WHERE :program IN (SELECT pr FROM Program p JOIN p.plans pr WHERE p.id = :programId)")
    long countPlansByProgramId(@Param("programId") UUID programId);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByNameIgnoreCase(String name);
}