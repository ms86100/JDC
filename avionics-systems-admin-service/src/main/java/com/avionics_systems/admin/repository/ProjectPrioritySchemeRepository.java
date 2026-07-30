package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ProjectPrioritySchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPrioritySchemeRepository extends JpaRepository<ProjectPrioritySchemeEntity, String> {
    Optional<ProjectPrioritySchemeEntity> findByProjectId(String projectId);
    List<ProjectPrioritySchemeEntity> findBySchemeId(String schemeId);
}
