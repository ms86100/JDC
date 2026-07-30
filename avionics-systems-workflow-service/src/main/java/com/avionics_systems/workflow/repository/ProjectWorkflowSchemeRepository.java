package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ProjectWorkflowScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProjectWorkflowSchemeRepository extends JpaRepository<ProjectWorkflowScheme, UUID> {
}
