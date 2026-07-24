package com.jira.issue.repository;

import com.jira.issue.entity.ChangeCardMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeCardMetadataRepository extends JpaRepository<ChangeCardMetadata, UUID> {

    Optional<ChangeCardMetadata> findByIssueId(UUID issueId);

    boolean existsByIssueId(UUID issueId);

    List<ChangeCardMetadata> findByChangeType(String changeType);

    List<ChangeCardMetadata> findByClassification(String classification);

    List<ChangeCardMetadata> findByParentDesignItemId(UUID parentDesignItemId);
}
