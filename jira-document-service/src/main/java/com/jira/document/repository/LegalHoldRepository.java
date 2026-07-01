package com.jira.document.repository;

import com.jira.document.entity.LegalHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LegalHoldRepository extends JpaRepository<LegalHold, UUID> {

    List<LegalHold> findByHoldType(String holdType);

    List<LegalHold> findByLegalMatterId(UUID legalMatterId);

    @Query("SELECT lh FROM LegalHold lh WHERE lh.status = :status")
    List<LegalHold> findByStatus(@Param("status") String status);

    @Query("SELECT lh FROM LegalHold lh WHERE lh.custodianIds @> :custodianId::uuid[]")
    List<LegalHold> findByCustodianContaining(@Param("custodianId") UUID custodianId);

    @Query("SELECT lh FROM LegalHold lh WHERE lh.status = 'ACTIVE' AND lh.endDate <= :date")
    List<LegalHold> findExpiringSoon(@Param("date") LocalDateTime date);

    @Query("SELECT lh FROM LegalHold lh WHERE lh.initiatedBy = :userId")
    List<LegalHold> findByInitiatedBy(@Param("userId") UUID userId);

    @Query("SELECT lh FROM LegalHold lh WHERE lh.projectIds @> :projectId::uuid[]")
    List<LegalHold> findByProjectContaining(@Param("projectId") UUID projectId);
}