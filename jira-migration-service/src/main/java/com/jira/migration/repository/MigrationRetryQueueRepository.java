package com.jira.migration.repository;

import com.jira.migration.entity.MigrationRetryQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MigrationRetryQueueRepository extends JpaRepository<MigrationRetryQueueEntry, UUID> {

    @Query("""
            SELECT e FROM MigrationRetryQueueEntry e
            WHERE e.status = 'PENDING' AND e.nextRetryAt <= :now
            ORDER BY e.nextRetryAt ASC
            """)
    List<MigrationRetryQueueEntry> findDue(LocalDateTime now);
}
