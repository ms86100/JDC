package com.jira.project.repository;

import com.jira.project.entity.TemplateCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TemplateCapabilityRepository extends JpaRepository<TemplateCapability, UUID> {

    List<TemplateCapability> findByTemplateIdOrderBySortOrderAsc(UUID templateId);

    List<TemplateCapability> findByTemplateIdInOrderBySortOrderAsc(List<UUID> templateIds);
}
