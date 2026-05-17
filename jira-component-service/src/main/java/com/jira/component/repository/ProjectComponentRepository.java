package com.jira.component.repository;

import com.jira.component.entity.ProjectComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectComponentRepository extends JpaRepository<ProjectComponent, UUID> {

    List<ProjectComponent> findByProjectIdAndDeletedFalseOrderBySequenceAsc(UUID projectId);

    Page<ProjectComponent> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    Optional<ProjectComponent> findByIdAndDeletedFalse(UUID id);

    @Query("SELECT c FROM ProjectComponent c WHERE c.projectId = :projectId AND c.deleted = false AND c.archived = false ORDER BY c.sequence ASC")
    List<ProjectComponent> findActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT c FROM ProjectComponent c WHERE c.deleted = false AND c.projectId = :projectId AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ProjectComponent> searchByName(@Param("projectId") UUID projectId, @Param("query") String query);

    @Query("SELECT COUNT(c) FROM ProjectComponent c WHERE c.projectId = :projectId AND c.deleted = false")
    long countByProjectId(@Param("projectId") UUID projectId);

    boolean existsByProjectIdAndNameAndIdNot(UUID projectId, String name, UUID excludeId);
}