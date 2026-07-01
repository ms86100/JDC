package com.jira.migration.repository.attachment;

import com.jira.migration.entity.attachment.AttachmentMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for attachment metadata operations.
 */
@Repository
public interface AttachmentMetadataRepository extends JpaRepository<AttachmentMetadataEntity, String> {

    /**
     * Find all attachments for an issue.
     */
    List<AttachmentMetadataEntity> findByIssueIdAndDeletedFalse(String issueId);

    /**
     * Find all attachments for an issue including deleted ones.
     */
    List<AttachmentMetadataEntity> findByIssueId(String issueId);

    /**
     * Count attachments for an issue.
     */
    long countByIssueIdAndDeletedFalse(String issueId);

    /**
     * Sum of all attachment sizes for an issue.
     */
    @Query("SELECT COALESCE(SUM(a.sizeBytes), 0) FROM AttachmentMetadataEntity a WHERE a.issueId = :issueId AND a.deleted = false")
    long sumSizeBytesByIssueId(@Param("issueId") String issueId);

    /**
     * Find attachment by filename and issue.
     */
    Optional<AttachmentMetadataEntity> findByFileNameAndIssueIdAndDeletedFalse(String fileName, String issueId);

    /**
     * Find attachment by original filename and issue.
     */
    Optional<AttachmentMetadataEntity> findByOriginalFileNameAndIssueIdAndDeletedFalse(String originalFileName, String issueId);

    /**
     * Delete all attachments for an issue (soft delete).
     */
    @Modifying
    @Query("UPDATE AttachmentMetadataEntity a SET a.deleted = true, a.deletedAt = :deletedAt WHERE a.issueId = :issueId")
    int softDeleteByIssueId(@Param("issueId") String issueId, @Param("deletedAt") Instant deletedAt);

    /**
     * Find attachments by storage path.
     */
    List<AttachmentMetadataEntity> findByStoragePathAndDeletedFalse(String storagePath);

    /**
     * Find attachments pending virus scan.
     */
    @Query("SELECT a FROM AttachmentMetadataEntity a WHERE a.virusScanStatus = 'PENDING' AND a.deleted = false")
    List<AttachmentMetadataEntity> findPendingVirusScans();

    /**
     * Check if attachment exists with given checksum.
     */
    boolean existsByContentHashAndDeletedFalse(String contentHash);

    /**
     * Find duplicate by content hash.
     */
    Optional<AttachmentMetadataEntity> findByContentHashAndDeletedFalse(String contentHash);
}