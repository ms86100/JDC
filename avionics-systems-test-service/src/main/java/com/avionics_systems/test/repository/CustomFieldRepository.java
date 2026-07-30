package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.CustomField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldRepository extends JpaRepository<CustomField, UUID> {

    List<CustomField> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<CustomField> findByFieldKey(String fieldKey);

    Optional<CustomField> findByProjectIdAndName(UUID projectId, String name);

    boolean existsByFieldKey(String fieldKey);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    List<CustomField> findByNameContainingIgnoreCase(String searchTerm);

    List<CustomField> findByFieldType(CustomField.FieldType fieldType);
}