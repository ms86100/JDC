package com.jira.issue.repository;

import com.jira.issue.entity.DclMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DclMetadataRepository extends JpaRepository<DclMetadata, UUID> {

    Optional<DclMetadata> findByIssueId(UUID issueId);

    boolean existsByIssueId(UUID issueId);

    List<DclMetadata> findBySupplierSyncProjectId(UUID supplierSyncProjectId);
}
