package com.jira.test.repository;

import com.jira.test.entity.AuditLog;
import com.jira.test.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByProjectIdOrderByActionTimestampDesc(UUID projectId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByActionTimestampDesc(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByActionTimestampDesc(UUID userId, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.projectId = :projectId AND a.actionTimestamp >= :since ORDER BY a.actionTimestamp DESC")
    List<AuditLog> findByProjectSince(@Param("projectId") UUID projectId, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a WHERE a.projectId = :projectId AND a.action IN :actions ORDER BY a.actionTimestamp DESC")
    List<AuditLog> findByProjectAndActions(@Param("projectId") UUID projectId, @Param("actions") List<AuditAction> actions);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.projectId = :projectId AND a.action = :action AND a.actionTimestamp >= :since")
    long countByProjectAndActionSince(@Param("projectId") UUID projectId, @Param("action") AuditAction action, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a WHERE a.projectId = :projectId AND a.action = :action AND a.actionTimestamp BETWEEN :start AND :end")
    List<AuditLog> findByProjectAndActionBetween(@Param("projectId") UUID projectId, @Param("action") AuditAction action, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
