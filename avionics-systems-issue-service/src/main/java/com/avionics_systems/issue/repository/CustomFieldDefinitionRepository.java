package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.CustomFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {

    Optional<CustomFieldDefinition> findByFieldKey(String fieldKey);

    List<CustomFieldDefinition> findByFieldType(String fieldType);

    List<CustomFieldDefinition> findByIsSearchableTrue();

    boolean existsByFieldKey(String fieldKey);

    List<CustomFieldDefinition> findByNameContainingIgnoreCase(String name);

    List<CustomFieldDefinition> findAllByOrderByNameAsc();
}
