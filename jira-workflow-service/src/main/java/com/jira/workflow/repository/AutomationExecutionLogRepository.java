package com.jira.workflow.repository;

import com.jira.workflow.entity.AutomationExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationExecutionLogRepository extends JpaRepository<AutomationExecutionLog, UUID> {

    List<AutomationExecutionLog> findByRuleIdOrderByExecutedAtDesc(UUID ruleId);

    long countByRuleId(UUID ruleId);
}
