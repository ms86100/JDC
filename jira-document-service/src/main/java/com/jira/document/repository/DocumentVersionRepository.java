package com.jira.document.repository;

import com.jira.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(UUID documentId, Integer versionNumber);

    @Query("SELECT MAX(dv.versionNumber) FROM DocumentVersion dv WHERE dv.documentId = :documentId")
    Optional<Integer> findMaxVersionNumberByDocumentId(@Param("documentId") UUID documentId);

    @Query("SELECT dv FROM DocumentVersion dv WHERE dv.documentId = :documentId ORDER BY dv.createdAt DESC")
    List<DocumentVersion> findByDocumentIdOrderByCreatedAtDesc(@Param("documentId") UUID documentId);
}