package com.jira.project.repository;

import com.jira.project.entity.ProjectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, UUID> {

    List<ProjectTemplate> findByTypeIdAndIsActiveTrueOrderBySortOrderAsc(UUID typeId);

    @Query("SELECT t FROM ProjectTemplate t WHERE t.type.id = :typeId AND t.isActive = true ORDER BY t.sortOrder ASC")
    List<ProjectTemplate> findActiveTemplatesByTypeId(@Param("typeId") UUID typeId);

    @Modifying
    @Query("DELETE FROM ProjectTemplate t WHERE t.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}