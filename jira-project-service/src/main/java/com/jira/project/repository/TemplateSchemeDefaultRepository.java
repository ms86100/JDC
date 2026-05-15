package com.jira.project.repository;

import com.jira.project.entity.TemplateSchemeDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateSchemeDefaultRepository extends JpaRepository<TemplateSchemeDefault, UUID> {

    Optional<TemplateSchemeDefault> findByTemplateId(UUID templateId);
}