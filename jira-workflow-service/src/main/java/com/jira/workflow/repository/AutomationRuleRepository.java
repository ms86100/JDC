package com.jira.workflow.repository;

import com.jira.workflow.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    List<AutomationRule> findByProjectIdAndIsEnabledTrue(UUID projectId);

    List<AutomationRule> findByTriggerTypeAndIsEnabledTrue(String triggerType);

    List<AutomationRule> findByIsEnabledTrueOrderByCreatedAtDesc();

    List<AutomationRule> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Find enabled rules matching a trigger type that apply to the given project (or are global).
     */
    @Query("SELECT r FROM AutomationRule r WHERE r.isEnabled = true AND r.triggerType = :triggerType " +
           "AND (r.projectId = :projectId OR r.projectId IS NULL) ORDER BY r.createdAt ASC")
    List<AutomationRule> findEnabledRulesForTrigger(
            @Param("triggerType") String triggerType,
            @Param("projectId") UUID projectId);
}
