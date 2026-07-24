package com.jira.issue.repository;

import com.jira.issue.entity.SystemStandardMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemStandardMetadataRepository extends JpaRepository<SystemStandardMetadata, UUID> {

    Optional<SystemStandardMetadata> findByIssueId(UUID issueId);

    boolean existsByIssueId(UUID issueId);
}
