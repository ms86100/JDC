package com.jira.migration.repository.field;

import com.jira.migration.entity.field.FieldVersionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for field version history.
 */
@Repository
public interface FieldVersionHistoryRepository extends JpaRepository<FieldVersionHistory, UUID> {

    /**
     * Find version history for a field, ordered by change time descending.
     */
    List<FieldVersionHistory> findByFieldDefinitionIdOrderByChangedAtDesc(UUID fieldDefinitionId);

    /**
     * Find a specific version of a field.
     */
    Optional<FieldVersionHistory> findByFieldDefinitionIdAndVersion(UUID fieldDefinitionId, Integer version);

    /**
     * Find all versions of a field with pagination.
     */
    Page<FieldVersionHistory> findByFieldDefinitionId(UUID fieldDefinitionId, Pageable pageable);

    /**
     * Find deleted fields in version history.
     */
    @Query("SELECT fvh FROM FieldVersionHistory fvh WHERE fvh.changeType = 'DELETED' ORDER BY fvh.changedAt DESC")
    List<FieldVersionHistory> findDeletedFields();

    /**
     * Count versions for a field.
     */
    long countByFieldDefinitionId(UUID fieldDefinitionId);
}