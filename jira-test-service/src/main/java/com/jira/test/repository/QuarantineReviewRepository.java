package com.jira.test.repository;

import com.jira.test.entity.QuarantineReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuarantineReviewRepository extends JpaRepository<QuarantineReview, UUID> {

    Optional<QuarantineReview> findByQuarantineId(UUID quarantineId);

    List<QuarantineReview> findByStatus(QuarantineReview.ReviewStatus status);

    List<QuarantineReview> findByCurrentReviewer(UUID reviewerId);

    List<QuarantineReview> findByStatusIn(List<QuarantineReview.ReviewStatus> statuses);

    List<QuarantineReview> findByNextReviewDateBefore(LocalDateTime date);

    List<QuarantineReview> findByJiraTicketKey(String jiraTicketKey);

    boolean existsByQuarantineId(UUID quarantineId);
}