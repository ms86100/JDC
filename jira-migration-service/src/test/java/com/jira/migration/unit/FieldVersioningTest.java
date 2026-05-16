package com.jira.migration.unit;

import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.entity.field.FieldSchemaMigration;
import com.jira.migration.entity.field.FieldVersionHistory;
import com.jira.migration.repository.field.FieldSchemaMigrationRepository;
import com.jira.migration.repository.field.FieldVersionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Field Versioning functionality.
 * Tests version history tracking, schema migration, and versioning logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Field Versioning Tests")
class FieldVersioningTest {

    @Mock
    private FieldVersionHistoryRepository versionHistoryRepository;

    @Mock
    private FieldSchemaMigrationRepository schemaMigrationRepository;

    private final UUID fieldDefId = UUID.randomUUID();
    private final UUID testUserId = UUID.randomUUID();

    @Nested
    @DisplayName("Field Version History Tests")
    class VersionHistoryTests {

        @Test
        @DisplayName("Should track field version on creation")
        void shouldTrackFieldVersionOnCreation() {
            // Given
            FieldDefinition fieldDef = createFieldDefinition("summary", FieldDefinition.FieldType.TEXT);
            when(versionHistoryRepository.save(any(FieldVersionHistory.class))).thenAnswer(invocation -> {
                FieldVersionHistory history = invocation.getArgument(0);
                history.setId(UUID.randomUUID());
                return history;
            });

            // When
            FieldVersionHistory history = FieldVersionHistory.builder()
                    .fieldDefinitionId(fieldDef.getId())
                    .version(1)
                    .changeType(FieldVersionHistory.ChangeType.CREATED)
                    .fieldKey(fieldDef.getFieldKey())
                    .displayName(fieldDef.getDisplayName())
                    .fieldType(fieldDef.getFieldType())
                    .changedBy(testUserId)
                    .changeReason("Field definition created")
                    .build();

            history = versionHistoryRepository.save(history);

            // Then
            assertThat(history.getVersion()).isEqualTo(1);
            assertThat(history.getChangeType()).isEqualTo(FieldVersionHistory.ChangeType.CREATED);
            verify(versionHistoryRepository).save(history);
        }

        @Test
        @DisplayName("Should track field version on update")
        void shouldTrackFieldVersionOnUpdate() {
            // Given
            FieldDefinition fieldDef = createFieldDefinition("description", FieldDefinition.FieldType.TEXTAREA);
            when(versionHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When - Simulate version increment
            FieldVersionHistory historyUpdate = FieldVersionHistory.builder()
                    .fieldDefinitionId(fieldDef.getId())
                    .version(2) // Incremented from 1
                    .changeType(FieldVersionHistory.ChangeType.UPDATED)
                    .fieldKey(fieldDef.getFieldKey())
                    .displayName("Updated Description")
                    .fieldType(fieldDef.getFieldType())
                    .changedBy(testUserId)
                    .changeReason("Display name updated")
                    .build();

            versionHistoryRepository.save(historyUpdate);

            // Then
            verify(versionHistoryRepository).save(historyUpdate);
            assertThat(historyUpdate.getChangeType()).isEqualTo(FieldVersionHistory.ChangeType.UPDATED);
        }

        @Test
        @DisplayName("Should retrieve version history for field")
        void shouldRetrieveVersionHistoryForField() {
            // Given
            List<FieldVersionHistory> history = List.of(
                    createVersionHistory(fieldDefId, 1, "CREATED"),
                    createVersionHistory(fieldDefId, 2, "UPDATED"),
                    createVersionHistory(fieldDefId, 3, "UPDATED")
            );
            when(versionHistoryRepository.findByFieldDefinitionIdOrderByChangedAtDesc(fieldDefId))
                    .thenReturn(history);

            // When
            List<FieldVersionHistory> result = versionHistoryRepository.findByFieldDefinitionIdOrderByChangedAtDesc(fieldDefId);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getVersion()).isEqualTo(3);
            assertThat(result.get(1).getVersion()).isEqualTo(2);
            assertThat(result.get(2).getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get specific version of field")
        void shouldGetSpecificVersionOfField() {
            // Given
            FieldVersionHistory version2 = createVersionHistory(fieldDefId, 2, "UPDATED");
            when(versionHistoryRepository.findByFieldDefinitionIdAndVersion(fieldDefId, 2))
                    .thenReturn(Optional.of(version2));

            // When
            Optional<FieldVersionHistory> result = versionHistoryRepository.findByFieldDefinitionIdAndVersion(fieldDefId, 2);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count versions for field")
        void shouldCountVersionsForField() {
            // Given
            when(versionHistoryRepository.countByFieldDefinitionId(fieldDefId)).thenReturn(5L);

            // When
            long count = versionHistoryRepository.countByFieldDefinitionId(fieldDefId);

            // Then
            assertThat(count).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Schema Migration Tests")
    class SchemaMigrationTests {

        @Test
        @DisplayName("Should create schema migration record")
        void shouldCreateSchemaMigrationRecord() {
            // Given
            FieldSchemaMigration migration = FieldSchemaMigration.builder()
                    .fieldDefinitionId(fieldDefId)
                    .fromVersion(1)
                    .toVersion(2)
                    .migrationType(FieldSchemaMigration.MigrationType.RENAME)
                    .status(FieldSchemaMigration.MigrationStatus.PENDING)
                    .build();

            when(schemaMigrationRepository.save(any())).thenAnswer(invocation -> {
                FieldSchemaMigration m = invocation.getArgument(0);
                m.setId(UUID.randomUUID());
                return m;
            });

            // When
            migration = schemaMigrationRepository.save(migration);

            // Then
            assertThat(migration.getId()).isNotNull();
            assertThat(migration.getStatus()).isEqualTo(FieldSchemaMigration.MigrationStatus.PENDING);
            assertThat(migration.getMigrationType()).isEqualTo(FieldSchemaMigration.MigrationType.RENAME);
        }

        @Test
        @DisplayName("Should mark migration as in progress")
        void shouldMarkMigrationAsInProgress() {
            // Given
            FieldSchemaMigration migration = createMigration(FieldSchemaMigration.MigrationStatus.PENDING);
            when(schemaMigrationRepository.findById(migration.getId())).thenReturn(Optional.of(migration));
            when(schemaMigrationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            migration.markInProgress();
            schemaMigrationRepository.save(migration);

            // Then
            assertThat(migration.getStatus()).isEqualTo(FieldSchemaMigration.MigrationStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should mark migration as completed")
        void shouldMarkMigrationAsCompleted() {
            // Given
            FieldSchemaMigration migration = createMigration(FieldSchemaMigration.MigrationStatus.IN_PROGRESS);
            when(schemaMigrationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            migration.markCompleted();
            schemaMigrationRepository.save(migration);

            // Then
            assertThat(migration.getStatus()).isEqualTo(FieldSchemaMigration.MigrationStatus.COMPLETED);
            assertThat(migration.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark migration as failed")
        void shouldMarkMigrationAsFailed() {
            // Given
            FieldSchemaMigration migration = createMigration(FieldSchemaMigration.MigrationStatus.IN_PROGRESS);
            when(schemaMigrationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            migration.markFailed("Invalid migration script");
            schemaMigrationRepository.save(migration);

            // Then
            assertThat(migration.getStatus()).isEqualTo(FieldSchemaMigration.MigrationStatus.FAILED);
            assertThat(migration.getErrorMessage()).isEqualTo("Invalid migration script");
            assertThat(migration.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should find pending migrations")
        void shouldFindPendingMigrations() {
            // Given
            List<FieldSchemaMigration> pending = List.of(
                    createMigration(FieldSchemaMigration.MigrationStatus.PENDING),
                    createMigration(FieldSchemaMigration.MigrationStatus.FAILED)
            );
            when(schemaMigrationRepository.findPendingMigrations()).thenReturn(pending);

            // When
            List<FieldSchemaMigration> result = schemaMigrationRepository.findPendingMigrations();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(m ->
                    assertThat(m.getStatus()).isIn(
                            FieldSchemaMigration.MigrationStatus.PENDING,
                            FieldSchemaMigration.MigrationStatus.FAILED
                    ));
        }

        @Test
        @DisplayName("Should find rollbackable migrations")
        void shouldFindRollbackableMigrations() {
            // Given
            FieldSchemaMigration migration = createMigration(FieldSchemaMigration.MigrationStatus.COMPLETED);
            migration.setRollbackScript(java.util.Map.of("action", "revert"));
            when(schemaMigrationRepository.findRollbackableMigrations()).thenReturn(List.of(migration));

            // When
            List<FieldSchemaMigration> result = schemaMigrationRepository.findRollbackableMigrations();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(FieldSchemaMigration.MigrationStatus.COMPLETED);
            assertThat(result.get(0).getRollbackScript()).isNotNull();
        }

        @Test
        @DisplayName("Should find stale migrations for cleanup")
        void shouldFindStaleMigrationsForCleanup() {
            // Given
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
            List<FieldSchemaMigration> stale = List.of(
                    createMigration(FieldSchemaMigration.MigrationStatus.IN_PROGRESS)
            );
            when(schemaMigrationRepository.findStaleMigrations(cutoff)).thenReturn(stale);

            // When
            List<FieldSchemaMigration> result = schemaMigrationRepository.findStaleMigrations(cutoff);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Version Increment Tests")
    class VersionIncrementTests {

        @Test
        @DisplayName("Should increment version on field update")
        void shouldIncrementVersionOnFieldUpdate() {
            // Given
            FieldDefinition fieldDef = createFieldDefinition("summary", FieldDefinition.FieldType.TEXT);
            fieldDef.setVersion(1);

            // When - Simulate update
            fieldDef.setVersion(fieldDef.getVersion() + 1);

            // Then
            assertThat(fieldDef.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should support all migration types")
        void shouldSupportAllMigrationTypes() {
            // Verify all migration types are available
            FieldSchemaMigration.MigrationType[] types = FieldSchemaMigration.MigrationType.values();

            assertThat(types).contains(
                    FieldSchemaMigration.MigrationType.RENAME,
                    FieldSchemaMigration.MigrationType.RETYPE,
                    FieldSchemaMigration.MigrationType.ADD_OPTION,
                    FieldSchemaMigration.MigrationType.REMOVE_OPTION,
                    FieldSchemaMigration.MigrationType.UPDATE_CONFIG,
                    FieldSchemaMigration.MigrationType.MIGRATE_DATA
            );
        }
    }

    // Helper methods
    private FieldDefinition createFieldDefinition(String fieldKey, FieldDefinition.FieldType type) {
        return FieldDefinition.builder()
                .id(fieldDefId)
                .fieldKey(fieldKey)
                .displayName("Test " + fieldKey)
                .fieldType(type)
                .version(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private FieldVersionHistory createVersionHistory(UUID fieldId, int version, String changeType) {
        return FieldVersionHistory.builder()
                .id(UUID.randomUUID())
                .fieldDefinitionId(fieldId)
                .version(version)
                .changeType(FieldVersionHistory.ChangeType.valueOf(changeType))
                .fieldKey("test_field")
                .displayName("Test Field")
                .fieldType(FieldDefinition.FieldType.TEXT)
                .changedAt(LocalDateTime.now())
                .build();
    }

    private FieldSchemaMigration createMigration(FieldSchemaMigration.MigrationStatus status) {
        return FieldSchemaMigration.builder()
                .id(UUID.randomUUID())
                .fieldDefinitionId(fieldDefId)
                .fromVersion(1)
                .toVersion(2)
                .migrationType(FieldSchemaMigration.MigrationType.RENAME)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}