package com.jira.issue.repository;

import com.jira.issue.entity.IssueLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueLinkRepository extends JpaRepository<IssueLink, UUID> {
    List<IssueLink> findBySourceIssueId(UUID sourceIssueId);
    List<IssueLink> findByDestinationIssueId(UUID destinationIssueId);
    List<IssueLink> findBySourceIssueIdOrDestinationIssueId(UUID sourceIssueId, UUID destinationIssueId);
    List<IssueLink> findByLinkType(String linkType);
    boolean existsBySourceIssueIdAndDestinationIssueIdAndLinkType(UUID sourceIssueId, UUID destinationIssueId, String linkType);
    void deleteBySourceIssueIdOrDestinationIssueId(UUID sourceIssueId, UUID destinationIssueId);
}