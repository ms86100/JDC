package com.jira.issue.repository;

import com.jira.issue.entity.TestEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestEvidenceRepository extends JpaRepository<TestEvidence, UUID> {

    List<TestEvidence> findByStepResultIdOrderByCreatedAtDesc(UUID stepResultId);

    List<TestEvidence> findByExecutionIdOrderByCreatedAtDesc(UUID executionId);

    List<TestEvidence> findByEvidenceType(String evidenceType);

    void deleteByStepResultId(UUID stepResultId);

    void deleteByExecutionId(UUID executionId);
}