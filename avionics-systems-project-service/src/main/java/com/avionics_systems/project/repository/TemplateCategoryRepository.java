package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateCategoryRepository extends JpaRepository<TemplateCategory, UUID> {

    List<TemplateCategory> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<TemplateCategory> findByCategoryKey(String categoryKey);
}
