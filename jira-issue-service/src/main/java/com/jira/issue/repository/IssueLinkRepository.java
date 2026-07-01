package com.jira.issue.repository;

import com.jira.issue.entity.IssueLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueLinkRepository extends JpaRepository<IssueLink, UUID> {
    List<IssueLink> findBySourceIssueId(UUID sourceIssueId);
    List<IssueLink> findByTargetIssueId(UUID targetIssueId);
    List<IssueLink> findBySourceIssueIdOrTargetIssueId(UUID sourceIssueId, UUID targetIssueId);
    List<IssueLink> findByLinkTypeId(UUID linkTypeId);
    boolean existsBySourceIssueIdAndTargetIssueIdAndLinkTypeId(UUID sourceIssueId, UUID targetIssueId, UUID linkTypeId);
    void deleteBySourceIssueIdOrTargetIssueId(UUID sourceIssueId, UUID targetIssueId);
}