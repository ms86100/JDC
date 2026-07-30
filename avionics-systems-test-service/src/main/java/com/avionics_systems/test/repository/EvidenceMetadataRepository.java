package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.EvidenceMetadata;
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