package com.jira.test.repository;

import com.jira.test.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComponentRepository extends JpaRepository<Component, UUID> {

    List<Component> findByProjectId(UUID projectId);

    Optional<Component> findByProjectIdAndComponentName(UUID projectId, String componentName);

    @Query("SELECT c FROM Component c WHERE c.projectId = :projectId AND " +
           "LOWER(c.componentName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Component> searchByName(@Param("projectId") UUID projectId, @Param("search") String search);

    @Query("SELECT c FROM Component c WHERE c.projectId = :projectId AND c.ownershipTeam = :team")
    List<Component> findByProjectIdAndOwnershipTeam(@Param("projectId") UUID projectId, @Param("team") String team);
}