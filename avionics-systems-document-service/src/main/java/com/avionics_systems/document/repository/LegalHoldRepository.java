package com.avionics_systems.document.repository;

import com.avionics_systems.document.entity.LegalHold;
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

    @Query(value = "SELECT * FROM legal_holds lh WHERE lh.custodian_ids @> ARRAY[CAST(:custodianId AS uuid)]", nativeQuery = true)
    List<LegalHold> findByCustodianContaining(@Param("custodianId") UUID custodianId);

    @Query("SELECT lh FROM LegalHold lh WHERE lh.status = :status AND lh.endDate <= :date")
    List<LegalHold> findExpiringSoon(@Param("status") String status, @Param("date") LocalDateTime date);

    @Query("SELECT lh FROM LegalHold lh WHERE lh.initiatedBy = :userId")
    List<LegalHold> findByInitiatedBy(@Param("userId") UUID userId);

    @Query(value = "SELECT * FROM legal_holds lh WHERE lh.project_ids @> ARRAY[CAST(:projectId AS uuid)]", nativeQuery = true)
    List<LegalHold> findByProjectContaining(@Param("projectId") UUID projectId);
}