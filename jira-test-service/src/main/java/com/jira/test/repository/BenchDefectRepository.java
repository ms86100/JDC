package com.jira.test.repository;

import com.jira.test.entity.BenchDefect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BenchDefectRepository extends JpaRepository<BenchDefect, UUID> {

    List<BenchDefect> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<BenchDefect> findBySourceTechEventId(UUID sourceTechEventId);

    List<BenchDefect> findByStatus(String status);

    List<BenchDefect> findBySeverity(String severity);

    Optional<BenchDefect> findByIssueKey(String issueKey);
}
