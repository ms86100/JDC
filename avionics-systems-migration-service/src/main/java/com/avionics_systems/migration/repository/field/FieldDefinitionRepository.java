package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.FieldDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, UUID> {

    Optional<FieldDefinition> findByFieldKey(String fieldKey);

    boolean existsByFieldKey(String fieldKey);

    List<FieldDefinition> findByPluginSource(String pluginSource);

    List<FieldDefinition> findByCustom(Boolean custom);

    List<FieldDefinition> findByDeprecated(Boolean deprecated);

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.screenRegion = :region")
    List<FieldDefinition> findByScreenRegion(FieldDefinition.ScreenRegion region);

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.searchable = true")
    List<FieldDefinition> findSearchableFields();

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.filterable = true")
    List<FieldDefinition> findFilterableFields();

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.sortable = true")
    List<FieldDefinition> findSortableFields();

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.required = true")
    List<FieldDefinition> findRequiredFields();

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.builtIn = true ORDER BY fd.displayName")
    List<FieldDefinition> findAllBuiltIn();

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.custom = true ORDER BY fd.displayName")
    List<FieldDefinition> findAllCustom();

    @Query("SELECT fd FROM FieldDefinition fd WHERE LOWER(fd.displayName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<FieldDefinition> searchByDisplayName(String query, Pageable pageable);

    @Query("SELECT fd FROM FieldDefinition fd WHERE LOWER(fd.fieldKey) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<FieldDefinition> searchByFieldKey(String query, Pageable pageable);

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.fieldType = :type")
    List<FieldDefinition> findByFieldType(FieldDefinition.FieldType type);

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.pluginSource IS NOT NULL")
    List<FieldDefinition> findAllPluginFields();

    @Query("SELECT fd FROM FieldDefinition fd WHERE fd.hidden = false AND fd.deprecated = false")
    List<FieldDefinition> findAllVisible();
}