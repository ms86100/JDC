package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.WorkflowScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowSchemeRepository extends JpaRepository<WorkflowScheme, UUID> {

    Optional<WorkflowScheme> findByIsDefaultTrue();

    Optional<WorkflowScheme> findByName(String name);

    List<WorkflowScheme> findByNameContainingIgnoreCase(String name);
}