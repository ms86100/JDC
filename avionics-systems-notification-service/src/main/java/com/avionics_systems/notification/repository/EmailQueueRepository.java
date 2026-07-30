package com.avionics_systems.notification.repository;

import com.avionics_systems.notification.entity.EmailQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, UUID> {

    @Query("SELECT q FROM EmailQueue q WHERE q.status = 'QUEUED' AND q.nextRetryAt <= :now ORDER BY q.createdAt ASC")
    List<EmailQueue> findReadyToSend(@Param("now") OffsetDateTime now);

    @Query("SELECT q FROM EmailQueue q WHERE q.status = 'FAILED' AND q.retryCount < q.maxRetries AND q.nextRetryAt <= :now ORDER BY q.nextRetryAt ASC")
    List<EmailQueue> findRetryable(@Param("now") OffsetDateTime now);

    Page<EmailQueue> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<EmailQueue> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);
}
