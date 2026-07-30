package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, UUID> {

    List<ProjectRole> findByProjectId(UUID projectId);

    Optional<ProjectRole> findByProjectIdAndName(UUID projectId, String name);

    Optional<ProjectRole> findByName(String name);

    @Modifying
    @Query("DELETE FROM ProjectRole pr WHERE pr.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}