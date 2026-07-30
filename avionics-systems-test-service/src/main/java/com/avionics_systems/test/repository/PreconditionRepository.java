package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.Precondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreconditionRepository extends JpaRepository<Precondition, UUID> {

    List<Precondition> findByProjectId(UUID projectId);

    List<Precondition> findByProjectIdAndStatus(UUID projectId, String status);

    Optional<Precondition> findById(UUID id);

    List<Precondition> findByProjectIdAndPreconditionType(UUID projectId, String preconditionType);

    List<Precondition> findByProjectIdAndCategory(UUID projectId, String category);

    List<Precondition> findByIdIn(List<UUID> ids);

    @Query("SELECT p FROM Precondition p WHERE p.projectId = :projectId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Precondition> searchByName(@Param("projectId") UUID projectId, @Param("query") String query);

    @Query("SELECT p FROM Precondition p WHERE p.projectId = :projectId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Precondition> fullTextSearch(@Param("projectId") UUID projectId, @Param("query") String query);

    @Query("SELECT p FROM Precondition p WHERE p.projectId = :projectId AND p.category = :category AND p.status = 'ACTIVE'")
    List<Precondition> findActiveByProjectAndCategory(@Param("projectId") UUID projectId, @Param("category") String category);

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, String status);
}