package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.SharedStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedStepRepository extends JpaRepository<SharedStep, UUID> {

    List<SharedStep> findByProjectIdAndArchivedFalse(UUID projectId);

    Optional<SharedStep> findByIdAndArchivedFalse(UUID id);

    List<SharedStep> findByProjectIdAndFolderIdAndArchivedFalse(UUID projectId, UUID folderId);

    boolean existsByProjectIdAndNameAndArchivedFalse(UUID projectId, String name);

    @Query("SELECT ss FROM SharedStep ss WHERE ss.projectId = :projectId AND ss.archived = false AND " +
           "(LOWER(ss.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ss.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<SharedStep> searchByNameOrDescription(@Param("projectId") UUID projectId, @Param("search") String search);

    List<SharedStep> findByProjectIdAndArchivedFalseOrderByUsageCountDesc(UUID projectId);

    @Query("SELECT ss FROM SharedStep ss WHERE ss.id IN :ids AND ss.archived = false")
    List<SharedStep> findAllByIdAndNotArchived(@Param("ids") List<UUID> ids);
}