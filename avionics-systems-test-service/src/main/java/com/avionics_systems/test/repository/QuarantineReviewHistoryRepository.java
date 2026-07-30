package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.QuarantineReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuarantineReviewHistoryRepository extends JpaRepository<QuarantineReviewHistory, UUID> {

    List<QuarantineReviewHistory> findByReviewIdOrderByCreatedAtDesc(UUID reviewId);

    List<QuarantineReviewHistory> findByQuarantineIdOrderByCreatedAtDesc(UUID quarantineId);

    List<QuarantineReviewHistory> findByActorIdOrderByCreatedAtDesc(UUID actorId);
}