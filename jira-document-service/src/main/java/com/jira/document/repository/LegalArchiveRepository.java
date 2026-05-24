package com.jira.document.repository;

import com.jira.document.entity.LegalArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LegalArchiveRepository extends JpaRepository<LegalArchive, UUID> {

    List<LegalArchive> findByProjectId(UUID projectId);

    List<LegalArchive> findByLegalMatterId(UUID legalMatterId);

    List<LegalArchive> findByStatus(String status);

    List<LegalArchive> findByArchiveType(String archiveType);

    @Query("SELECT la FROM LegalArchive la WHERE la.status = :status AND la.retentionDate <= :date")
    List<LegalArchive> findReadyForDisposition(@Param("status") String status, @Param("date") java.time.LocalDateTime date);

    @Query("SELECT la FROM LegalArchive la WHERE la.archivedBy = :userId")
    List<LegalArchive> findByArchivedBy(@Param("userId") UUID userId);
}