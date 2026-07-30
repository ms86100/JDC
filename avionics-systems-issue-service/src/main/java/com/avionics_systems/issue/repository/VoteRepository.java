package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    List<Vote> findByIssueId(UUID issueId);
    List<Vote> findByUserId(UUID userId);
    Optional<Vote> findByIssueIdAndUserId(UUID issueId, UUID userId);
    boolean existsByIssueIdAndUserId(UUID issueId, UUID userId);
    void deleteByIssueIdAndUserId(UUID issueId, UUID userId);
    long countByIssueId(UUID issueId);
}