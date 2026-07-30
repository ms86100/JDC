package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.SystemStandardMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemStandardMetadataRepository extends JpaRepository<SystemStandardMetadata, UUID> {

    Optional<SystemStandardMetadata> findByIssueId(UUID issueId);

    boolean existsByIssueId(UUID issueId);
}
