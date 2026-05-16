package com.jira.plan.repository;

import com.jira.plan.entity.LexoRankAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LexoRankAuditLogRepository extends JpaRepository<LexoRankAuditLog, UUID> {
    List<LexoRankAuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, UUID entityId);
    Page<LexoRankAuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);
    List<LexoRankAuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}