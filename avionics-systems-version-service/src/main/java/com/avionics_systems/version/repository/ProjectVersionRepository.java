package com.avionics_systems.version.repository;

import com.avionics_systems.version.entity.ProjectVersion;
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
public interface ProjectVersionRepository extends JpaRepository<ProjectVersion, UUID> {

    List<ProjectVersion> findByProjectIdAndDeletedFalseOrderBySequenceAsc(UUID projectId);

    Page<ProjectVersion> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    Optional<ProjectVersion> findByIdAndDeletedFalse(UUID id);

    @Query("SELECT v FROM ProjectVersion v WHERE v.projectId = :projectId AND v.deleted = false AND v.archived = false ORDER BY v.sequence ASC")
    List<ProjectVersion> findActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT v FROM ProjectVersion v WHERE v.projectId = :projectId AND v.deleted = false AND v.released = false AND v.archived = false ORDER BY v.releaseDate ASC NULLS LAST")
    List<ProjectVersion> findUnreleasedByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT v FROM ProjectVersion v WHERE v.projectId = :projectId AND v.deleted = false AND v.released = true ORDER BY v.actualReleaseDate DESC")
    List<ProjectVersion> findReleasedByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT v FROM ProjectVersion v WHERE v.projectId = :projectId AND v.deleted = false AND v.releaseTrain = :trainName ORDER BY v.sequence ASC")
    List<ProjectVersion> findByProjectIdAndReleaseTrain(@Param("projectId") UUID projectId, @Param("trainName") String trainName);

    @Query("SELECT v FROM ProjectVersion v WHERE v.deleted = false AND v.projectId = :projectId AND (LOWER(v.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ProjectVersion> searchByName(@Param("projectId") UUID projectId, @Param("query") String query);

    @Query("SELECT COUNT(v) FROM ProjectVersion v WHERE v.projectId = :projectId AND v.deleted = false")
    long countByProjectId(@Param("projectId") UUID projectId);

    boolean existsByProjectIdAndNameAndIdNot(UUID projectId, String name, UUID excludeId);

    boolean existsByProjectIdAndName(UUID projectId, String name);
}