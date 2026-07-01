package com.jira.migration.service;

import com.jira.migration.entity.MigrationAuditEntry;
import com.jira.migration.repository.MigrationAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists migration audit trail to migration_audit table (P5-02 / P9-01).
 */
@Service
@RequiredArgsConstructor
public class MigrationAuditPersistenceService {

    private final MigrationAuditRepository migrationAuditRepository;

    @Transactional
    public void log(UUID jobId, String action, String entityType, String entityKey,
                    UUID performedBy, Map<String, Object> details) {
        Map<String, Object> merged = details != null ? new HashMap<>(details) : new HashMap<>();
        if (jobId != null) {
            merged.put("jobId", jobId.toString());
        }
        migrationAuditRepository.save(MigrationAuditEntry.builder()
                .jobId(jobId)
                .action(action)
                .entityType(entityType)
                .entityKey(entityKey)
                .performedBy(performedBy)
                .details(merged)
                .build());
    }

    @Transactional(readOnly = true)
    public List<MigrationAuditEntry> getJobTrail(UUID jobId) {
        return migrationAuditRepository.findByJobIdOrderByPerformedAtAsc(jobId);
    }
}
