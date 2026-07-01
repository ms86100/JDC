package com.jira.migration.entity.field;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Field schema migration entity for managing field definition changes.
 * Provides support for complex schema transformations with rollback capability.
 */
@Entity
@Table(name = "field_schema_migrations", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSchemaMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_definition_id", nullable = false)
    private UUID fieldDefinitionId;

    @Column(name = "from_version", nullable = false)
    private Integer fromVersion;

    @Column(name = "to_version", nullable = false)
    private Integer toVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "migration_type", nullable = false, length = 50)
    private MigrationType migrationType;

    @Column(name = "migration_script", columnDefinition = "jsonb")
    private Map<String, Object> migrationScript;

    @Column(name = "rollback_script", columnDefinition = "jsonb")
    private Map<String, Object> rollbackScript;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private MigrationStatus status = MigrationStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum MigrationType {
        RENAME,
        RETYPE,
        ADD_OPTION,
        REMOVE_OPTION,
        UPDATE_CONFIG,
        ADD_VALIDATION,
        REMOVE_VALIDATION,
        ADD_FIELD,
        REMOVE_FIELD,
        MIGRATE_DATA,
        SCHEMA_EVOLUTION
    }

    public enum MigrationStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        ROLLED_BACK
    }

    public void markInProgress() {
        this.status = MigrationStatus.IN_PROGRESS;
    }

    public void markCompleted() {
        this.status = MigrationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.status = MigrationStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = LocalDateTime.now();
    }

    public void markRolledBack() {
        this.status = MigrationStatus.ROLLED_BACK;
        this.completedAt = LocalDateTime.now();
    }
}