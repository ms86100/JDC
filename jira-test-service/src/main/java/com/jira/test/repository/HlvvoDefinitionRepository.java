package com.jira.test.repository;

import com.jira.test.entity.HlvvoDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HlvvoDefinitionRepository extends JpaRepository<HlvvoDefinition, UUID> {

    List<HlvvoDefinition> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<HlvvoDefinition> findByIssueKey(String issueKey);

    List<HlvvoDefinition> findByProjectIdAndStatus(UUID projectId, String status);

    long countByProjectId(UUID projectId);

    List<HlvvoDefinition> findByProjectId(UUID projectId);
}
