package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findByUserId(UUID userId);

    List<ProjectMember> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);

    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}