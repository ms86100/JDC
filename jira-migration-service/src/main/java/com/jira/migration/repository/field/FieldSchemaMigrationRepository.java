package com.jira.migration.repository.field;

import com.jira.migration.entity.field.FieldSchemaMigration;
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
 * Repository for field schema migrations.
 */
@Repository
public interface FieldSchemaMigrationRepository extends JpaRepository<FieldSchemaMigration, UUID> {

    /**
     * Find pending migrations for execution.
     */
    @Query("SELECT fsm FROM FieldSchemaMigration fsm WHERE fsm.status IN ('PENDING', 'FAILED') ORDER BY fsm.createdAt ASC")
    List<FieldSchemaMigration> findPendingMigrations();

    /**
     * Find migrations in progress.
     */
    List<FieldSchemaMigration> findByStatus(FieldSchemaMigration.MigrationStatus status);

    /**
     * Find migrations for a specific field.
     */
    List<FieldSchemaMigration> findByFieldDefinitionIdOrderByCreatedAtDesc(UUID fieldDefinitionId);

    /**
     * Find migrations that can be rolled back.
     */
    @Query("SELECT fsm FROM FieldSchemaMigration fsm WHERE fsm.status = 'COMPLETED' AND fsm.rollbackScript IS NOT NULL ORDER BY fsm.createdAt DESC")
    List<FieldSchemaMigration> findRollbackableMigrations();

    /**
     * Find stale in-progress migrations (cleanup for crashed instances).
     */
    @Query("SELECT fsm FROM FieldSchemaMigration fsm WHERE fsm.status = 'IN_PROGRESS' AND fsm.createdAt < :cutoff")
    List<FieldSchemaMigration> findStaleMigrations(LocalDateTime cutoff);

    /**
     * Delete migrations for a field.
     */
    @Modifying
    @Query("DELETE FROM FieldSchemaMigration fsm WHERE fsm.fieldDefinitionId = :fieldDefId")
    void deleteByFieldDefinitionId(UUID fieldDefId);
}