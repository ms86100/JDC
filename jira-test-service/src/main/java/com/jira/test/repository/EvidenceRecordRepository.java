package com.jira.test.repository;

import com.jira.test.entity.EvidenceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRecordRepository extends JpaRepository<EvidenceRecord, UUID> {

    List<EvidenceRecord> findByExecutionIdOrderByCreatedAtDesc(UUID executionId);

    List<EvidenceRecord> findByExecutionIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID executionId);

    List<EvidenceRecord> findByStepResultIdOrderByCreatedAtDesc(UUID stepResultId);

    List<EvidenceRecord> findByEvidenceType(String evidenceType);

    List<EvidenceRecord> findByClassificationLevel(String classificationLevel);

    List<EvidenceRecord> findByExecutionIdAndEvidenceType(UUID executionId, String evidenceType);

    List<EvidenceRecord> findByRetentionPolicyId(UUID retentionPolicyId);

    long countByExecutionId(UUID executionId);

    long countByEvidenceType(String evidenceType);
}