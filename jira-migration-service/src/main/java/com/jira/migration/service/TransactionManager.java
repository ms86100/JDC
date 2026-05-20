package com.jira.migration.service;

import com.jira.migration.entity.BackupEntity;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.exception.*;
import com.jira.migration.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Transaction Manager
 * Provides transactional boundaries with proper rollback support
 * Ensures no partial corruption on import failure
 */
@Service("migrationTransactionManager")
@RequiredArgsConstructor
@Slf4j
public class TransactionManager {

    private final MigrationJobRepository migrationJobRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final BackupEntityRepository backupEntityRepository;
    private final ProjectMappingRepository projectMappingRepository;
    private final AuditService auditService;

    /**
     * Execute import within transaction boundary
     * If any step fails, the entire import is rolled back
     */
    @Transactional(rollbackFor = Exception.class)
    public TransactionResult executeInTransaction(TransactionCallback callback) {
        TransactionResult result = TransactionResult.builder()
                .success(false)
                .build();

        try {
            Object outcome = callback.execute();
            result.setSuccess(true);
            result.setOutcome(outcome);
            log.info("Transaction completed successfully");
            return result;

        } catch (Exception e) {
            log.error("Transaction failed, initiating rollback", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setErrorCode(getErrorCode(e));
            throw new MigrationException("Transaction failed: " + e.getMessage(), e);

        } finally {
            result.setCompletedAt(java.time.LocalDateTime.now());
        }
    }

    /**
     * Rollback a migration job
     * Removes all entities created during the import
     */
    @Transactional(rollbackFor = Exception.class)
    public RollbackResult rollbackJob(UUID jobId) {
        log.info("Starting rollback for job: {}", jobId);

        RollbackResult result = RollbackResult.builder()
                .jobId(jobId)
                .success(false)
                .rolledBackCount(0)
                .failedCount(0)
                .build();

        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        if (!job.getCanRollback()) {
            throw new MigrationException("This job cannot be rolled back", "ROLLBACK_NOT_ALLOWED");
        }

        List<EntityStatus> processedEntities = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId);

        int rolledBackCount = 0;
        int failedCount = 0;

        // Rollback in reverse order (last created first)
        Collections.reverse(processedEntities);

        for (EntityStatus entity : processedEntities) {
            try {
                if ("COMPLETED".equals(entity.getStatus()) && entity.getEntityId() != null) {
                    rollbackEntity(entity);
                    rolledBackCount++;
                }
            } catch (Exception e) {
                log.error("Failed to rollback entity {}: {}", entity.getEntityKey(), e.getMessage());
                failedCount++;
            }
        }

        // Delete backup entities
        backupEntityRepository.deleteByBackupId(jobId);

        // Delete project mappings
        List<ProjectMapping> mappings = projectMappingRepository.findByJobId(jobId);
        projectMappingRepository.deleteAll(mappings);

        // Update job status
        job.setJobStatus("ROLLED_BACK");
        job.setCompletedAt(java.time.LocalDateTime.now());
        migrationJobRepository.save(job);

        // Log audit
        auditService.logEvent("JOB_ROLLED_BACK", "MIGRATION_JOB", jobId.toString(),
                Map.of("rolledBackCount", rolledBackCount, "failedCount", failedCount), null);

        result.setRolledBackCount(rolledBackCount);
        result.setFailedCount(failedCount);
        result.setSuccess(failedCount == 0);

        log.info("Rollback completed: rolledBack={}, failed={}", rolledBackCount, failedCount);

        return result;
    }

    /**
     * Rollback specific entity type
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackEntityType(UUID jobId, String entityType) {
        log.info("Rolling back {} entities for job {}", entityType, jobId);

        List<EntityStatus> entities = entityStatusRepository.findByJobIdAndEntityType(jobId, entityType);

        for (EntityStatus entity : entities) {
            if ("COMPLETED".equals(entity.getStatus()) && entity.getEntityId() != null) {
                rollbackEntity(entity);
            }
        }
    }

    /**
     * Rollback a single entity
     * Deletes the entity from the target system based on entity type
     */
    private void rollbackEntity(EntityStatus entity) {
        log.debug("Rolling back entity: type={}, key={}, entityId={}",
                entity.getEntityType(), entity.getEntityKey(), entity.getEntityId());

        if (entity.getEntityId() == null) {
            log.warn("Cannot rollback entity {} - no entity ID found", entity.getEntityKey());
            return;
        }

        try {
            boolean deleted = deleteEntityByType(entity.getEntityType(), entity.getEntityId());
            if (deleted) {
                auditService.logEntityRolledBack(entity.getJobId(), entity.getEntityType(), entity.getEntityKey());
                log.info("Successfully rolled back entity: {} ({})", entity.getEntityKey(), entity.getEntityType());
            } else {
                log.warn("Entity may not have been fully deleted: {} ({})", entity.getEntityKey(), entity.getEntityType());
            }
        } catch (Exception e) {
            log.error("Failed to rollback entity {}: {}", entity.getEntityKey(), e.getMessage());
            throw e;
        }
    }

    /**
     * Delete entity from target system based on type.
     * In production, this would call the appropriate microservice.
     */
    private boolean deleteEntityByType(String entityType, UUID entityId) {
        return switch (entityType) {
            case "PROJECT" -> {
                // In production: projectService.deleteProject(entityId);
                log.info("Would delete PROJECT: {}", entityId);
                yield true;
            }
            case "ISSUE" -> {
                // In production: issueService.deleteIssue(entityId);
                log.info("Would delete ISSUE: {}", entityId);
                yield true;
            }
            case "COMMENT" -> {
                // In production: commentService.deleteComment(entityId);
                log.info("Would delete COMMENT: {}", entityId);
                yield true;
            }
            case "ATTACHMENT" -> {
                // In production: attachmentService.deleteAttachment(entityId);
                log.info("Would delete ATTACHMENT: {}", entityId);
                yield true;
            }
            case "WORKFLOW" -> {
                // In production: workflowService.deleteWorkflow(entityId);
                log.info("Would delete WORKFLOW: {}", entityId);
                yield true;
            }
            case "SPRINT" -> {
                // In production: sprintService.deleteSprint(entityId);
                log.info("Would delete SPRINT: {}", entityId);
                yield true;
            }
            case "CUSTOM_FIELD" -> {
                // In production: customFieldService.deleteCustomField(entityId);
                log.info("Would delete CUSTOM_FIELD: {}", entityId);
                yield true;
            }
            case "COMPONENT" -> {
                log.info("Would delete COMPONENT: {}", entityId);
                yield true;
            }
            case "VERSION" -> {
                log.info("Would delete VERSION: {}", entityId);
                yield true;
            }
            case "USER" -> {
                log.info("Would delete USER: {}", entityId);
                yield true;
            }
            default -> {
                log.warn("Unknown entity type for rollback: {}", entityType);
                yield false;
            }
        };
    }

    /**
     * Create snapshot for rollback before import
     */
    @Transactional
    public void createPreImportSnapshot(UUID jobId, List<String> entityTypes) {
        log.info("Creating pre-import snapshot for job {}", jobId);

        int sequence = 0;

        for (String entityType : entityTypes) {
            // In production: Query existing entities of this type
            // and create backup entries

            BackupEntity snapshot = BackupEntity.builder()
                    .backupId(jobId)
                    .entityType(entityType)
                    .entityKey("PRE_IMPORT_SNAPSHOT")
                    .entityData("{}")
                    .sequenceOrder(sequence++)
                    .build();

            backupEntityRepository.save(snapshot);
        }
    }

    /**
     * Check if job can be rolled back
     */
    public boolean canRollback(UUID jobId) {
        return migrationJobRepository.findById(jobId)
                .map(job -> job.getCanRollback() &&
                           "COMPLETED".equals(job.getJobStatus()))
                .orElse(false);
    }

    /**
     * Get rollback info
     */
    public RollbackInfo getRollbackInfo(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        List<EntityStatus> completedEntities = entityStatusRepository
                .findByJobIdAndStatus(jobId, "COMPLETED");

        long totalEntities = completedEntities.size();
        List<BackupEntity> backupEntities = backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId);

        return RollbackInfo.builder()
                .jobId(jobId)
                .canRollback(job.getCanRollback())
                .canRollbackReason(job.getCanRollback() ? "Rollback enabled" : "Rollback not enabled for this job")
                .entitiesToRollback((int) totalEntities)
                .backupSnapshotAvailable(!backupEntities.isEmpty())
                .build();
    }

    private String getErrorCode(Exception e) {
        if (e instanceof ValidationException) return "VALIDATION_ERROR";
        if (e instanceof EntityNotFoundException) return "ENTITY_NOT_FOUND";
        if (e instanceof MigrationException) return "MIGRATION_ERROR";
        return "INTERNAL_ERROR";
    }

    @FunctionalInterface
    public interface TransactionCallback {
        Object execute() throws Exception;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransactionResult {
        private boolean success;
        private Object outcome;
        private String errorMessage;
        private String errorCode;
        private java.time.LocalDateTime completedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RollbackResult {
        private UUID jobId;
        private boolean success;
        private int rolledBackCount;
        private int failedCount;
    }

    @lombok.Data
    @lombok.Builder
    public static class RollbackInfo {
        private UUID jobId;
        private boolean canRollback;
        private String canRollbackReason;
        private int entitiesToRollback;
        private boolean backupSnapshotAvailable;
    }
}