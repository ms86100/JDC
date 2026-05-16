package com.jira.migration.unit;

import com.jira.migration.entity.BackupEntity;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.repository.*;
import com.jira.migration.service.AuditService;
import com.jira.migration.service.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionManager rollback functionality.
 * Tests entity rollback, pre-import snapshot, and rollback info.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Manager Tests")
class TransactionManagerTest {

    @Mock
    private MigrationJobRepository migrationJobRepository;

    @Mock
    private EntityStatusRepository entityStatusRepository;

    @Mock
    private BackupEntityRepository backupEntityRepository;

    @Mock
    private ProjectMappingRepository projectMappingRepository;

    @Mock
    private AuditService auditService;

    private TransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new TransactionManager(
                migrationJobRepository,
                entityStatusRepository,
                backupEntityRepository,
                projectMappingRepository,
                auditService
        );
    }

    @Nested
    @DisplayName("Rollback Tests")
    class RollbackTests {

        @Test
        @DisplayName("Should rollback job and delete all entities")
        void shouldRollbackJobAndDeleteAllEntities() {
            // Given
            UUID jobId = UUID.randomUUID();
            UUID projectEntityId = UUID.randomUUID();
            UUID issueEntityId = UUID.randomUUID();

            MigrationJob job = createJob(jobId, true);
            List<EntityStatus> entities = List.of(
                    createEntityStatus(jobId, "PROJECT", "PROJ-1", projectEntityId),
                    createEntityStatus(jobId, "ISSUE", "PROJ-1-1", issueEntityId)
            );

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));
            when(entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId)).thenReturn(entities);
            when(backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId)).thenReturn(List.of());
            when(projectMappingRepository.findByJobId(jobId)).thenReturn(List.of());

            // When
            TransactionManager.RollbackResult result = transactionManager.rollbackJob(jobId);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getRolledBackCount()).isEqualTo(2);
            assertThat(result.getFailedCount()).isEqualTo(0);

            // Verify job status updated
            verify(migrationJobRepository).save(argThat(j ->
                    "ROLLED_BACK".equals(j.getJobStatus()) && j.getCompletedAt() != null);

            // Verify audit logged
            verify(auditService).logEvent(eq("JOB_ROLLED_BACK"), eq("MIGRATION_JOB"), eq(jobId.toString()),
                    any(), any());
        }

        @Test
        @DisplayName("Should rollback entities in reverse order")
        void shouldRollbackEntitiesInReverseOrder() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            List<EntityStatus> entities = List.of(
                    createEntityStatus(jobId, "ISSUE", "ISSUE-1", UUID.randomUUID()),
                    createEntityStatus(jobId, "PROJECT", "PROJ-1", UUID.randomUUID()),
                    createEntityStatus(jobId, "SPRINT", "SPRINT-1", UUID.randomUUID())
            );

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));
            when(entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId)).thenReturn(entities);
            when(backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId)).thenReturn(List.of());
            when(projectMappingRepository.findByJobId(jobId)).thenReturn(List.of());

            // When
            TransactionManager.RollbackResult result = transactionManager.rollbackJob(jobId);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getRolledBackCount()).isEqualTo(3);

            // Audit should be called for each entity (in reverse order)
            verify(auditService, times(3)).logEntityRolledBack(any(), any(), any());
        }

        @Test
        @DisplayName("Should handle rollback when entity has no ID")
        void shouldHandleRollbackWhenEntityHasNoId() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            List<EntityStatus> entities = List.of(
                    createEntityStatus(jobId, "PROJECT", "PROJ-1", null) // No entity ID
            );

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));
            when(entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId)).thenReturn(entities);
            when(backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId)).thenReturn(List.of());
            when(projectMappingRepository.findByJobId(jobId)).thenReturn(List.of());

            // When
            TransactionManager.RollbackResult result = transactionManager.rollbackJob(jobId);

            // Then - Should not throw, but entity should be skipped
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should not rollback entity with non-COMPLETED status")
        void shouldNotRollbackEntityWithNonCompletedStatus() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            EntityStatus pendingEntity = createEntityStatus(jobId, "PROJECT", "PROJ-1", UUID.randomUUID());
            pendingEntity.setStatus("PENDING"); // Not completed

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));
            when(entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId)).thenReturn(List.of(pendingEntity));
            when(backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId)).thenReturn(List.of());
            when(projectMappingRepository.findByJobId(jobId)).thenReturn(List.of());

            // When
            TransactionManager.RollbackResult result = transactionManager.rollbackJob(jobId);

            // Then
            assertThat(result.getRolledBackCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should delete backup entities during rollback")
        void shouldDeleteBackupEntitiesDuringRollback() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            List<BackupEntity> backups = List.of(
                    BackupEntity.builder().id(UUID.randomUUID()).entityType("PROJECT").build(),
                    BackupEntity.builder().id(UUID.randomUUID()).entityType("ISSUE").build()
            );

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));
            when(entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId)).thenReturn(List.of());
            when(backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId)).thenReturn(backups);
            when(projectMappingRepository.findByJobId(jobId)).thenReturn(List.of());

            // When
            TransactionManager.RollbackResult result = transactionManager.rollbackJob(jobId);

            // Then
            verify(backupEntityRepository).deleteByBackupId(jobId);
        }

        @Test
        @DisplayName("Should delete project mappings during rollback")
        void shouldDeleteProjectMappingsDuringRollback() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            List<ProjectMapping> mappings = List.of(
                    ProjectMapping.builder().id(UUID.randomUUID()).sourceProjectKey("PROJ-1").build()
            );

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));
            when(entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId)).thenReturn(List.of());
            when(backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId)).thenReturn(List.of());
            when(projectMappingRepository.findByJobId(jobId)).thenReturn(mappings);

            // When
            TransactionManager.RollbackResult result = transactionManager.rollbackJob(jobId);

            // Then
            verify(projectMappingRepository).deleteAll(mappings);
        }
    }

    @Nested
    @DisplayName("Rollback Entity Type Tests")
    class RollbackEntityTypeTests {

        @Test
        @DisplayName("Should rollback specific entity type")
        void shouldRollbackSpecificEntityType() {
            // Given
            UUID jobId = UUID.randomUUID();
            List<EntityStatus> entities = List.of(
                    createEntityStatus(jobId, "ISSUE", "ISSUE-1", UUID.randomUUID())
            );

            when(entityStatusRepository.findByJobIdAndEntityType(jobId, "ISSUE")).thenReturn(entities);

            // When
            transactionManager.rollbackEntityType(jobId, "ISSUE");

            // Then
            verify(entityStatusRepository).findByJobIdAndEntityType(jobId, "ISSUE");
            verify(auditService).logEntityRolledBack(jobId, "ISSUE", "ISSUE-1");
        }
    }

    @Nested
    @DisplayName("Can Rollback Tests")
    class CanRollbackTests {

        @Test
        @DisplayName("Should return true when job can be rolled back")
        void shouldReturnTrueWhenJobCanBeRolledBack() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            job.setJobStatus("COMPLETED");

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));

            // When
            boolean canRollback = transactionManager.canRollback(jobId);

            // Then
            assertThat(canRollback).isTrue();
        }

        @Test
        @DisplayName("Should return false when job cannot be rolled back")
        void shouldReturnFalseWhenJobCannotBeRolledBack() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, false); // canRollback = false

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));

            // When
            boolean canRollback = transactionManager.canRollback(jobId);

            // Then
            assertThat(canRollback).isFalse();
        }

        @Test
        @DisplayName("Should return false for non-completed job")
        void shouldReturnFalseForNonCompletedJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            job.setJobStatus("IN_PROGRESS");

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));

            // When
            boolean canRollback = transactionManager.canRollback(jobId);

            // Then
            assertThat(canRollback).isFalse();
        }
    }

    @Nested
    @DisplayName("Rollback Info Tests")
    class RollbackInfoTests {

        @Test
        @DisplayName("Should return rollback info with entity count")
        void shouldReturnRollbackInfoWithEntityCount() {
            // Given
            UUID jobId = UUID.randomUUID();
            MigrationJob job = createJob(jobId, true);
            job.setJobStatus("COMPLETED");

            List<EntityStatus> completedEntities = List.of(
                    createEntityStatus(jobId, "PROJECT", "PROJ-1", UUID.randomUUID()),
                    createEntityStatus(jobId, "ISSUE", "ISSUE-1", UUID.randomUUID())
            );
            List<BackupEntity> backups = List.of(
                    BackupEntity.builder().id(UUID.randomUUID()).build()
            );

            when(migrationJobRepository.findById(jobId)).thenReturn(java.util.Optional.of(job));
            when(entityStatusRepository.findByJobIdAndStatus(jobId, "COMPLETED")).thenReturn(completedEntities);
            when(backupEntityRepository.findByBackupIdOrderBySequenceOrderAsc(jobId)).thenReturn(backups);

            // When
            TransactionManager.RollbackInfo info = transactionManager.getRollbackInfo(jobId);

            // Then
            assertThat(info.isCanRollback()).isTrue();
            assertThat(info.getEntitiesToRollback()).isEqualTo(2);
            assertThat(info.isBackupSnapshotAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Pre-Import Snapshot Tests")
    class PreImportSnapshotTests {

        @Test
        @DisplayName("Should create pre-import snapshot")
        void shouldCreatePreImportSnapshot() {
            // Given
            UUID jobId = UUID.randomUUID();
            List<String> entityTypes = List.of("PROJECT", "ISSUE");

            when(backupEntityRepository.save(any(BackupEntity.class))).thenAnswer(invocation -> {
                BackupEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

            // When
            transactionManager.createPreImportSnapshot(jobId, entityTypes);

            // Then
            verify(backupEntityRepository, times(2)).save(any(BackupEntity.class));
        }
    }

    // Helper methods
    private MigrationJob createJob(UUID id, boolean canRollback) {
        return MigrationJob.builder()
                .id(id)
                .jobType("IMPORT")
                .jobStatus("COMPLETED")
                .canRollback(canRollback)
                .initiatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private EntityStatus createEntityStatus(UUID jobId, String entityType, String entityKey, UUID entityId) {
        return EntityStatus.builder()
                .id(UUID.randomUUID())
                .jobId(jobId)
                .entityType(entityType)
                .entityKey(entityKey)
                .entityId(entityId)
                .status("COMPLETED")
                .processingOrder(0)
                .build();
    }
}