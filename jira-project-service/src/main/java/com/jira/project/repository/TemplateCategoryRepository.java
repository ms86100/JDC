package com.jira.project.repository;

import com.jira.project.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateCategoryRepository extends JpaRepository<TemplateCategory, UUID> {

    List<TemplateCategory> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<TemplateCategory> findByCategoryKey(String categoryKey);
}
