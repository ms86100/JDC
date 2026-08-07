package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.VvmCardMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VvmCardMetadataRepository extends JpaRepository<VvmCardMetadata, UUID> {

    Optional<VvmCardMetadata> findByIssueId(UUID issueId);
}
