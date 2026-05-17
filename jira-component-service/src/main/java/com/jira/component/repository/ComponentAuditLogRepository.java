package com.jira.component.repository;

import com.jira.component.entity.ComponentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentAuditLogRepository extends JpaRepository<ComponentAuditLog, UUID> {

    List<ComponentAuditLog> findByComponentIdOrderByCreatedAtDesc(UUID componentId);

    Page<ComponentAuditLog> findByComponentId(UUID componentId, Pageable pageable);

    List<ComponentAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}