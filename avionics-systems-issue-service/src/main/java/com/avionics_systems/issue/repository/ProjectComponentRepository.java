package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.ProjectComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectComponentRepository extends JpaRepository<ProjectComponent, UUID> {
    List<ProjectComponent> findByProjectId(UUID projectId);
    Optional<ProjectComponent> findByProjectIdAndName(UUID projectId, String name);
    boolean existsByProjectIdAndName(UUID projectId, String name);
}