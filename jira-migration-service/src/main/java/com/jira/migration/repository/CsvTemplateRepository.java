package com.jira.migration.repository;

import com.jira.migration.entity.CsvTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CsvTemplateRepository extends JpaRepository<CsvTemplate, UUID> {

    Optional<CsvTemplate> findByTemplateNameAndVersion(String templateName, String version);

    List<CsvTemplate> findByEntityType(String entityType);

    Optional<CsvTemplate> findByTemplateName(String templateName);

    boolean existsByTemplateName(String templateName);

    List<CsvTemplate> findByCreatedBy(UUID createdBy);
}