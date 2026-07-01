package com.jira.plan.repository;

import com.jira.plan.entity.BoardConfigAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardConfigAuditLogRepository extends JpaRepository<BoardConfigAuditLog, Long> {

    List<BoardConfigAuditLog> findByBoardIdOrderByCreatedAtDesc(UUID boardId);

    List<BoardConfigAuditLog> findByBoardIdAndEventTypeOrderByCreatedAtDesc(UUID boardId, String eventType);
}