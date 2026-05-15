package com.jira.issue.repository;

import com.jira.issue.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabelRepository extends JpaRepository<Label, UUID> {
    Optional<Label> findByName(String name);
    Optional<Label> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    List<Label> findByIssueId(UUID issueId);
    boolean existsByIssueIdAndNameIgnoreCase(UUID issueId, String name);
    void deleteByIssueIdAndNameIgnoreCase(UUID issueId, String name);
}