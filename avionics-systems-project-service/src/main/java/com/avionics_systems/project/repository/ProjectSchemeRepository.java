package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ProjectScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectSchemeRepository extends JpaRepository<ProjectScheme, UUID> {

    Optional<ProjectScheme> findByProjectId(UUID projectId);

    boolean existsByProjectId(UUID projectId);
}