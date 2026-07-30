package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.VvoDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VvoDefinitionRepository extends JpaRepository<VvoDefinition, UUID> {

    List<VvoDefinition> findByProjectIdAndArchivedFalseOrderByCreatedAtDesc(UUID projectId);

    List<VvoDefinition> findByHlvvoId(UUID hlvvoId);

    List<VvoDefinition> findByFixVersionId(UUID fixVersionId);

    Optional<VvoDefinition> findByIdDoors(String idDoors);

    Optional<VvoDefinition> findByIssueKey(String issueKey);

    List<VvoDefinition> findByProjectIdAndStatusIn(UUID projectId, List<String> statuses);

    boolean existsByIssueKey(String issueKey);

    boolean existsByIdDoors(String idDoors);

    long countByProjectIdAndStatus(UUID projectId, String status);

    long countByProjectId(UUID projectId);

    List<VvoDefinition> findByProjectIdAndArchivedFalse(UUID projectId);
}
