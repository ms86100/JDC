package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.DlqEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Dead Letter Queue entries.
 * Provides persistence and query capabilities for failed operations.
 */
@Repository
public interface DlqEntryRepository extends JpaRepository<DlqEntry, UUID> {

    /**
     * Find all pending DLQ entries with pagination.
     */
    @Query("SELECT d FROM DlqEntry d WHERE d.status IN ('PENDING', 'SCHEDULED') ORDER BY d.firstFailure ASC")
    Page<DlqEntry> findPending(Pageable pageable);

    /**
     * Find all pending DLQ entries for a specific job.
     */
    @Query("SELECT d FROM DlqEntry d WHERE d.jobId = :jobId AND d.status IN ('PENDING', 'SCHEDULED') ORDER BY d.firstFailure ASC")
    List<DlqEntry> findPendingByJobId(UUID jobId);

    /**
     * Find DLQ entries eligible for retry.
     */
    @Query("SELECT d FROM DlqEntry d WHERE d.status = 'SCHEDULED' AND d.nextRetry <= :now")
    List<DlqEntry> findEligibleForRetry(LocalDateTime now);

    /**
     * Find all DLQ entries for an entity.
     */
    @Query("SELECT d FROM DlqEntry d WHERE d.entityType = :entityType AND d.entityKey = :entityKey ORDER BY d.firstFailure DESC")
    List<DlqEntry> findByEntity(String entityType, String entityKey);

    /**
     * Find DLQ entries by status.
     */
    List<DlqEntry> findByStatus(DlqEntry.DlqStatus status);

    /**
     * Find DLQ entries by operation type.
     */
    List<DlqEntry> findByOperationType(String operationType);

    /**
     * Count pending entries.
     */
    @Query("SELECT COUNT(d) FROM DlqEntry d WHERE d.status IN ('PENDING', 'SCHEDULED')")
    long countPending();

    /**
     * Delete entries older than a date.
     */
    @Modifying
    @Query("DELETE FROM DlqEntry d WHERE d.firstFailure < :cutoff AND d.status IN ('COMPLETED', 'DISCARDED')")
    int deleteOldEntries(LocalDateTime cutoff);

    /**
     * Find entries ready for auto-retry.
     */
    @Query("SELECT d FROM DlqEntry d WHERE d.status = 'PENDING' AND d.attemptCount < :maxAttempts")
    List<DlqEntry> findForAutoRetry(int maxAttempts);

    /**
     * Bulk update status for entries.
     */
    @Modifying
    @Query("UPDATE DlqEntry d SET d.status = :newStatus WHERE d.id IN :ids")
    int bulkUpdateStatus(List<UUID> ids, DlqEntry.DlqStatus newStatus);
}