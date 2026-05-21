package com.jira.migration.repository;

import com.jira.migration.entity.DcStagingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DcStagingEntryRepository extends JpaRepository<DcStagingEntry, UUID> {

    List<DcStagingEntry> findByJobIdOrderBySequenceOrderAsc(UUID jobId);

    List<DcStagingEntry> findByJobIdAndEntityTypeOrderBySequenceOrderAsc(UUID jobId, String entityType);

    List<DcStagingEntry> findByJobIdAndValidationState(UUID jobId, String validationState);

    void deleteByJobId(UUID jobId);

    long countByJobIdAndImportBatchIdAndChecksum(UUID jobId, UUID importBatchId, String checksum);
}
