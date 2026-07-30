package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.Initiative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InitiativeRepository extends JpaRepository<Initiative, UUID> {

    List<Initiative> findByOwnerId(UUID ownerId);

    List<Initiative> findByProgramId(UUID programId);

    List<Initiative> findByIsActiveTrue();

    @Query("SELECT i FROM Initiative i LEFT JOIN FETCH i.epics WHERE i.id = :id")
    Optional<Initiative> findByIdWithEpics(@Param("id") UUID id);

    @Query("SELECT i FROM Initiative i LEFT JOIN FETCH i.plans WHERE i.id = :id")
    Optional<Initiative> findByIdWithPlans(@Param("id") UUID id);

    @Query("SELECT i FROM Initiative i LEFT JOIN FETCH i.epics LEFT JOIN FETCH i.plans WHERE i.id = :id")
    Optional<Initiative> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT i FROM Initiative i WHERE i.isActive = true ORDER BY i.createdAt DESC")
    List<Initiative> findAllActiveOrderByCreatedAtDesc();

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByNameIgnoreCase(String name);
}