package com.jira.issue.repository;

import com.jira.issue.entity.DeliverableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliverableMetadataRepository extends JpaRepository<DeliverableMetadata, UUID> {

    Optional<DeliverableMetadata> findByIssueId(UUID issueId);

    boolean existsByIssueId(UUID issueId);

    List<DeliverableMetadata> findByDeliverableType(String deliverableType);

    List<DeliverableMetadata> findByMilestoneType(String milestoneType);
}
