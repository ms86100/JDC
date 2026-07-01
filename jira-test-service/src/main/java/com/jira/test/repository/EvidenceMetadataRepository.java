package com.jira.test.repository;

import com.jira.test.entity.EvidenceMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceMetadataRepository extends JpaRepository<EvidenceMetadata, UUID> {

    List<EvidenceMetadata> findByEvidenceIdOrderByCreatedAtAsc(UUID evidenceId);

    List<EvidenceMetadata> findByEvidenceIdAndMetadataKey(UUID evidenceId, String metadataKey);

    void deleteByEvidenceId(UUID evidenceId);
}