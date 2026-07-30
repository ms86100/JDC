package com.avionics_systems.notification.repository;

import com.avionics_systems.notification.entity.AutomationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationActionRepository extends JpaRepository<AutomationAction, UUID> {

    List<AutomationAction> findByRuleId(UUID ruleId);

    @Query("SELECT aa FROM AutomationAction aa WHERE aa.ruleId = :ruleId AND aa.enabled = true ORDER BY aa.orderIndex")
    List<AutomationAction> findEnabledByRuleId(@Param("ruleId") UUID ruleId);

    @Modifying
    @Query("DELETE FROM AutomationAction aa WHERE aa.ruleId = :ruleId")
    void deleteAllByRuleId(@Param("ruleId") UUID ruleId);
}