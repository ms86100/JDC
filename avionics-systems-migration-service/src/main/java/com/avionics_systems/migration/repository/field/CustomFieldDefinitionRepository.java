package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.CustomFieldDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {

    Optional<CustomFieldDefinition> findByName(String name);

    Optional<CustomFieldDefinition> findByFieldKey(String fieldKey);

    boolean existsByName(String name);

    boolean existsByFieldKey(String fieldKey);

    List<CustomFieldDefinition> findByEnabled(Boolean enabled);

    List<CustomFieldDefinition> findBySearchable(Boolean searchable);

    @Query("SELECT cfd FROM CustomFieldDefinition cfd WHERE cfd.enabled = true ORDER BY cfd.name")
    List<CustomFieldDefinition> findAllEnabled();

    @Query("SELECT cfd FROM CustomFieldDefinition cfd WHERE cfd.type = :type")
    List<CustomFieldDefinition> findByType(String type);

    @Query("SELECT cfd FROM CustomFieldDefinition cfd WHERE cfd.searcherKey = :searcherKey")
    List<CustomFieldDefinition> findBySearcherKey(String searcherKey);

    @Query("SELECT cfd FROM CustomFieldDefinition cfd WHERE LOWER(cfd.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<CustomFieldDefinition> searchByName(String query, Pageable pageable);

    @Query("SELECT cfd FROM CustomFieldDefinition cfd WHERE cfd.searchable = true AND cfd.enabled = true")
    List<CustomFieldDefinition> findAllSearchableEnabled();

    @Query("SELECT cfd FROM CustomFieldDefinition cfd WHERE cfd.navigable = true AND cfd.enabled = true")
    List<CustomFieldDefinition> findAllNavigableEnabled();
}