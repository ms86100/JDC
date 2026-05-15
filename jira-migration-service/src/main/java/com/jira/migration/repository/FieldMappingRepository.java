package com.jira.migration.repository;

import com.jira.migration.entity.FieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMapping, UUID> {

    List<FieldMapping> findByMappingType(String mappingType);

    List<FieldMapping> findBySourceTypeAndTargetType(String sourceType, String targetType);

    List<FieldMapping> findByIsSharedTrue();

    List<FieldMapping> findByCreatedBy(UUID createdBy);

    Optional<FieldMapping> findByMappingNameAndMappingType(String mappingName, String mappingType);

    boolean existsByMappingName(String mappingName);
}