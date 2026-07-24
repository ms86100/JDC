package com.jira.test.repository;

import com.jira.test.entity.TechEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TechEventRepository extends JpaRepository<TechEvent, UUID> {

    List<TechEvent> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<TechEvent> findByStatus(String status);

    List<TechEvent> findByDetectedOnProgramId(UUID detectedOnProgramId);

    Optional<TechEvent> findByIssueKey(String issueKey);

    List<TechEvent> findBySystemSupplierId(UUID systemSupplierId);

    List<TechEvent> findByReporterTeamId(UUID reporterTeamId);

    List<TechEvent> findByProjectIdAndStatusIn(UUID projectId, List<String> statuses);

    long countByProjectIdAndStatus(UUID projectId, String status);
}
