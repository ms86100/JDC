package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ProjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectTypeRepository extends JpaRepository<ProjectType, UUID> {

    List<ProjectType> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<ProjectType> findByName(String name);

    Optional<ProjectType> findByCategory(String category);
}