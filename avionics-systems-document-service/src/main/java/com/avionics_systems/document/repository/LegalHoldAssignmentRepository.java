package com.avionics_systems.document.repository;

import com.avionics_systems.document.entity.LegalHoldAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LegalHoldAssignmentRepository extends JpaRepository<LegalHoldAssignment, UUID> {

    List<LegalHoldAssignment> findByLegalHoldId(UUID legalHoldId);

    List<LegalHoldAssignment> findByUserId(UUID userId);

    List<LegalHoldAssignment> findByProjectId(UUID projectId);

    List<LegalHoldAssignment> findByIssueId(UUID issueId);

    @Query("SELECT lha FROM LegalHoldAssignment lha WHERE lha.userId = :userId AND lha.acknowledged = false")
    List<LegalHoldAssignment> findUnacknowledgedByUserId(@Param("userId") UUID userId);

    @Query("SELECT lha FROM LegalHoldAssignment lha WHERE lha.legalHoldId = :holdId AND lha.acknowledged = false")
    List<LegalHoldAssignment> findUnacknowledgedByHoldId(@Param("holdId") UUID legalHoldId);

    boolean existsByLegalHoldIdAndUserId(UUID legalHoldId, UUID userId);

    boolean existsByLegalHoldIdAndProjectId(UUID legalHoldId, UUID projectId);
}