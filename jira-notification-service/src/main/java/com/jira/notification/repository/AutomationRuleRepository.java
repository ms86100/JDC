package com.jira.notification.repository;

import com.jira.notification.entity.AutomationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    List<AutomationRule> findByProjectId(UUID projectId);

    List<AutomationRule> findByEnabled(Boolean enabled);

    @Query("SELECT ar FROM AutomationRule ar WHERE ar.projectId = :projectId AND ar.enabled = true ORDER BY ar.orderIndex")
    List<AutomationRule> findEnabledByProjectId(@Param("projectId") UUID projectId);

    Page<AutomationRule> findByCreatedBy(UUID createdBy, Pageable pageable);

    @Query("SELECT ar FROM AutomationRule ar WHERE ar.isSystemRule = false AND ar.enabled = true ORDER BY ar.orderIndex")
    List<AutomationRule> findAllActiveUserRules();

    @Modifying
    @Query("UPDATE AutomationRule ar SET ar.executionCount = ar.executionCount + 1, ar.lastExecutedAt = CURRENT_TIMESTAMP, ar.lastStatus = :status WHERE ar.id = :ruleId")
    void updateExecutionStats(@Param("ruleId") UUID ruleId, @Param("status") String status);

    @Modifying
    @Query("UPDATE AutomationRule ar SET ar.enabled = :enabled WHERE ar.id = :ruleId")
    int updateEnabledById(@Param("ruleId") UUID ruleId, @Param("enabled") Boolean enabled);

    @Query("SELECT ar FROM AutomationRule ar WHERE ar.projectId IS NULL AND ar.enabled = true ORDER BY ar.orderIndex")
    List<AutomationRule> findGlobalActiveRules();
}