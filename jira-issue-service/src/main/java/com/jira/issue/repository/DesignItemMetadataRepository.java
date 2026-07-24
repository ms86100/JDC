package com.jira.issue.repository;

import com.jira.issue.entity.DesignItemMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DesignItemMetadataRepository extends JpaRepository<DesignItemMetadata, UUID> {

    Optional<DesignItemMetadata> findByIssueId(UUID issueId);

    boolean existsByIssueId(UUID issueId);
}
