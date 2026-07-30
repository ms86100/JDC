package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.BackupEntity;
import com.avionics_systems.migration.entity.EntityStatus;
import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.repository.BackupEntityRepository;
import com.avionics_systems.migration.repository.EntityStatusRepository;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Audit Service
 * Provides comprehensive audit logging for all migration operations
 * Tracks who did what, when, and the outcome
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final MigrationJobRepository migrationJobRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final BackupEntityRepository backupEntityRepository;

    // In production, inject actual audit repository
    private final List<AuditEvent> auditTrail = new ArrayList<>();

    /**
     * Log audit event
     */
    public void logEvent(String action, String entityType, String entityKey,
                        Map<String, Object> details, UUID performedBy) {
        AuditEvent event = AuditEvent.builder()
                .action(action)
                .entityType(entityType)
                .entityKey(entityKey)
                .details(details)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditTrail.add(event);
        log.debug("Audit: {} {} {} by {}", action, entityType, entityKey, performedBy);
    }

    /**
     * Log import started
     */
    public void logImportStarted(UUID jobId, String importType, UUID initiatedBy) {
        logEvent("IMPORT_STARTED", "MIGRATION_JOB", jobId.toString(),
                Map.of("importType", importType), initiatedBy);

        MigrationJob job = migrationJobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setConfig(merge(job.getConfig(), Map.of("auditTrail", List.of())));
            migrationJobRepository.save(job);
        }
    }

    /**
     * Log import completed
     */
    public void logImportCompleted(UUID jobId, int successCount, int failureCount) {
        logEvent("IMPORT_COMPLETED", "MIGRATION_JOB", jobId.toString(),
                Map.of("successCount", successCount, "failureCount", failureCount), null);
    }

    /**
     * Log import failed
     */
    public void logImportFailed(UUID jobId, String errorMessage) {
        logEvent("IMPORT_FAILED", "MIGRATION_JOB", jobId.toString(),
                Map.of("errorMessage", errorMessage), null);
    }

    /**
     * Log entity processed
     */
    public void logEntityProcessed(UUID jobId, String entityType, String entityKey,
                                  boolean success, String errorMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put("success", success);
        if (errorMessage != null) {
            details.put("errorMessage", errorMessage);
        }

        logEvent("ENTITY_PROCESSED", entityType, entityKey, details, null);
    }

    /**
     * Log entity rolled back
     */
    public void logEntityRolledBack(UUID jobId, String entityType, String entityKey) {
        logEvent("ENTITY_ROLLED_BACK", entityType, entityKey,
                Map.of("reason", "Import failed - transactional rollback"), null);
    }

    /**
     * Log user mapping created
     */
    public void logUserMappingCreated(UUID jobId, String sourceUser, String targetUser, UUID createdBy) {
        logEvent("USER_MAPPING_CREATED", "USER_MAPPING", sourceUser,
                Map.of("targetUser", targetUser), createdBy);
    }

    /**
     * Log field mapping applied
     */
    public void logFieldMappingApplied(UUID jobId, String sourceField, String targetField) {
        logEvent("FIELD_MAPPING_APPLIED", "FIELD_MAPPING", sourceField,
                Map.of("targetField", targetField), null);
    }

    /**
     * Get audit trail for a job
     */
    @Transactional(readOnly = true)
    public List<AuditEvent> getJobAuditTrail(UUID jobId) {
        return auditTrail.stream()
                .filter(e -> jobId.toString().equals(e.getDetails().get("jobId")))
                .toList();
    }

    /**
     * Get audit summary for a job
     */
    @Transactional(readOnly = true)
    public AuditSummary getJobAuditSummary(UUID jobId) {
        List<AuditEvent> jobEvents = getJobAuditTrail(jobId);

        Map<String, Integer> actionCounts = new HashMap<>();
        for (AuditEvent event : jobEvents) {
            actionCounts.merge(event.getAction(), 1, Integer::sum);
        }

        long successCount = jobEvents.stream()
                .filter(e -> "ENTITY_PROCESSED".equals(e.getAction()) &&
                        Boolean.TRUE.equals(e.getDetails().get("success")))
                .count();

        long failureCount = jobEvents.stream()
                .filter(e -> "ENTITY_PROCESSED".equals(e.getAction()) &&
                        e.getDetails().containsKey("errorMessage"))
                .count();

        return AuditSummary.builder()
                .jobId(jobId)
                .totalEvents(jobEvents.size())
                .actionCounts(actionCounts)
                .successCount((int) successCount)
                .failureCount((int) failureCount)
                .build();
    }

    /**
     * Generate audit report for export
     */
    @Transactional(readOnly = true)
    public String generateAuditReport(UUID jobId) {
        AuditSummary summary = getJobAuditSummary(jobId);
        List<AuditEvent> events = getJobAuditTrail(jobId);

        StringBuilder report = new StringBuilder();
        report.append("=== Migration Audit Report ===\n");
        report.append("Job ID: ").append(jobId).append("\n");
        report.append("Total Events: ").append(summary.getTotalEvents()).append("\n");
        report.append("Success: ").append(summary.getSuccessCount()).append("\n");
        report.append("Failures: ").append(summary.getFailureCount()).append("\n\n");

        report.append("Action Breakdown:\n");
        for (Map.Entry<String, Integer> entry : summary.getActionCounts().entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        report.append("\nDetailed Events:\n");
        for (AuditEvent event : events) {
            report.append(String.format("  [%s] %s %s %s - %s\n",
                    event.getPerformedAt(),
                    event.getAction(),
                    event.getEntityType(),
                    event.getEntityKey(),
                    event.getDetails()));
        }

        return report.toString();
    }

    private Map<String, Object> merge(Map<String, Object> existing, Map<String, Object> newData) {
        if (existing == null) return newData != null ? newData : new HashMap<>();
        Map<String, Object> result = new HashMap<>(existing);
        if (newData != null) {
            result.putAll(newData);
        }
        return result;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuditEvent {
        private String action;
        private String entityType;
        private String entityKey;
        private Map<String, Object> details;
        private UUID performedBy;
        private LocalDateTime performedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuditSummary {
        private UUID jobId;
        private int totalEvents;
        private Map<String, Integer> actionCounts;
        private int successCount;
        private int failureCount;
    }
}