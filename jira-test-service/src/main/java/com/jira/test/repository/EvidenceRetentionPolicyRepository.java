package com.jira.test.repository;

import com.jira.test.entity.EvidenceRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRetentionPolicyRepository extends JpaRepository<EvidenceRetentionPolicy, UUID> {

    List<EvidenceRetentionPolicy> findByProjectId(UUID projectId);

    List<EvidenceRetentionPolicy> findByProjectIdAndIsActiveTrue(UUID projectId);

    List<EvidenceRetentionPolicy> findByEvidenceType(String evidenceType);

    List<EvidenceRetentionPolicy> findByIsActiveTrue();

    List<EvidenceRetentionPolicy> findByAutoArchiveTrue();
}