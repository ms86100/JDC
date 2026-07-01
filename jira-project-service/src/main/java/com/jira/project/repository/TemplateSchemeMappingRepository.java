package com.jira.project.repository;

import com.jira.project.entity.TemplateSchemeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateSchemeMappingRepository extends JpaRepository<TemplateSchemeMapping, UUID> {

    List<TemplateSchemeMapping> findByTemplateId(UUID templateId);

    List<TemplateSchemeMapping> findByTemplateIdAndSchemeType(UUID templateId, String schemeType);

    Optional<TemplateSchemeMapping> findByTemplateIdAndSchemeTypeAndIsDefaultTrue(UUID templateId, String schemeType);

    Optional<TemplateSchemeMapping> findByTemplateIdAndSchemeTypeAndSchemeName(UUID templateId, String schemeType, String schemeName);

    void deleteByTemplateId(UUID templateId);
}