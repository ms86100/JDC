package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.SprintAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SprintAuditLogRepository extends JpaRepository<SprintAuditLog, Long> {

    List<SprintAuditLog> findBySprintIdOrderByCreatedAtDesc(UUID sprintId);

    List<SprintAuditLog> findBySprintIdAndEventType(UUID sprintId, String eventType);
}