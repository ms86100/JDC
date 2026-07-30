package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.ChangeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChangeGroupRepository extends JpaRepository<ChangeGroup, UUID> {
    List<ChangeGroup> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
    void deleteByIssueId(UUID issueId);
}