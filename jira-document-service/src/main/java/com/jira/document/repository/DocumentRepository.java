package com.jira.document.repository;

import com.jira.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwnerId(UUID ownerId);

    List<Document> findByProjectId(UUID projectId);

    List<Document> findByIssueId(UUID issueId);

    Page<Document> findByOwnerId(UUID ownerId, Pageable pageable);

    List<Document> findByParentDocumentId(UUID parentDocumentId);

    @Query("SELECT d FROM Document d WHERE d.projectId = :projectId AND d.isArchived = false")
    List<Document> findActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT d FROM Document d WHERE d.space = :space AND d.isArchived = false")
    List<Document> findBySpace(@Param("space") String space);

    @Query("SELECT d FROM Document d WHERE d.documentType = :documentType AND d.isArchived = false")
    List<Document> findByDocumentType(@Param("documentType") String documentType);

    @Query("SELECT d FROM Document d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) AND d.isArchived = false")
    List<Document> searchByTitle(@Param("query") String query);
}