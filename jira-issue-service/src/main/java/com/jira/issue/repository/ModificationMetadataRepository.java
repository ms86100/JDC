package com.jira.issue.repository;

import com.jira.issue.entity.ModificationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModificationMetadataRepository extends JpaRepository<ModificationMetadata, UUID> {

    Optional<ModificationMetadata> findByIssueId(UUID issueId);

    boolean existsByIssueId(UUID issueId);

    List<ModificationMetadata> findByModType(String modType);
}
