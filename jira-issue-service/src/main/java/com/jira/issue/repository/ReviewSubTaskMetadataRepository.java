package com.jira.issue.repository;

import com.jira.issue.entity.ReviewSubTaskMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewSubTaskMetadataRepository extends JpaRepository<ReviewSubTaskMetadata, UUID> {

    Optional<ReviewSubTaskMetadata> findByIssueId(UUID issueId);

    List<ReviewSubTaskMetadata> findByParentSystemStandardId(UUID parentSystemStandardId);

    boolean existsByIssueId(UUID issueId);
}
