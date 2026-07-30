package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.IssueFieldValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueFieldValueRepository extends JpaRepository<IssueFieldValue, UUID> {

    List<IssueFieldValue> findByIssueId(UUID issueId);

    Optional<IssueFieldValue> findByIssueIdAndFieldDefinitionId(UUID issueId, UUID fieldDefinitionId);

    @Query("SELECT ifv FROM IssueFieldValue ifv WHERE ifv.issueId = :issueId ORDER BY ifv.fieldDefinitionId")
    List<IssueFieldValue> findByIssueIdOrderByFieldDefinitionId(UUID issueId);

    @Query("SELECT ifv FROM IssueFieldValue ifv WHERE ifv.validationStatus = :status")
    Page<IssueFieldValue> findByValidationStatus(IssueFieldValue.ValidationStatus status, Pageable pageable);

    @Query("SELECT ifv FROM IssueFieldValue ifv WHERE ifv.importedFrom = :source")
    List<IssueFieldValue> findByImportedFrom(String source);

    @Query("SELECT ifv FROM IssueFieldValue ifv WHERE ifv.importMappingId = :mappingId")
    List<IssueFieldValue> findByImportMappingId(UUID mappingId);

    @Query("SELECT ifv FROM IssueFieldValue ifv WHERE ifv.searchableText LIKE %:query%")
    Page<IssueFieldValue> searchBySearchableText(String query, Pageable pageable);

    @Modifying
    @Query("DELETE FROM IssueFieldValue ifv WHERE ifv.issueId = :issueId")
    void deleteByIssueId(UUID issueId);

    @Modifying
    @Query("DELETE FROM IssueFieldValue ifv WHERE ifv.issueId = :issueId AND ifv.fieldDefinitionId = :fieldDefId")
    void deleteByIssueIdAndFieldDefinitionId(UUID issueId, UUID fieldDefId);

    @Query("SELECT COUNT(ifv) FROM IssueFieldValue ifv WHERE ifv.issueId = :issueId")
    long countByIssueId(UUID issueId);

    @Query("SELECT DISTINCT ifv.issueId FROM IssueFieldValue ifv WHERE ifv.fieldDefinitionId = :fieldDefId")
    List<UUID> findIssueIdsByFieldDefinitionId(UUID fieldDefId);

    @Query("SELECT ifv FROM IssueFieldValue ifv JOIN FETCH ifv.fieldDefinition WHERE ifv.issueId = :issueId")
    List<IssueFieldValue> findByIssueIdWithFieldDefinition(UUID issueId);

    @Query("SELECT ifv FROM IssueFieldValue ifv WHERE ifv.fieldDefinitionId = :fieldDefId")
    List<IssueFieldValue> findByFieldDefinitionId(UUID fieldDefId);

    @Modifying
    @Query("DELETE FROM IssueFieldValue ifv WHERE ifv.fieldDefinitionId = :fieldDefId")
    void deleteByFieldDefinitionId(UUID fieldDefId);
}