package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ScriptExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScriptExecutionLogRepository extends JpaRepository<ScriptExecutionLog, UUID> {

    Page<ScriptExecutionLog> findByScriptIdOrderByCreatedAtDesc(UUID scriptId, Pageable pageable);

    Page<ScriptExecutionLog> findByScriptKeyOrderByCreatedAtDesc(String scriptKey, Pageable pageable);

    Page<ScriptExecutionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    void deleteByCreatedAtBefore(java.time.LocalDateTime cutoff);

    long countByCreatedAtBefore(java.time.LocalDateTime cutoff);
}
