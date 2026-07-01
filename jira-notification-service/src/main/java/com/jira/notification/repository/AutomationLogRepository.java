package com.jira.notification.repository;

import com.jira.notification.entity.AutomationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationLogRepository extends JpaRepository<AutomationLog, UUID> {

    Page<AutomationLog> findByRuleId(UUID ruleId, Pageable pageable);

    @Query("SELECT al FROM AutomationLog al WHERE al.ruleId = :ruleId ORDER BY al.createdAt DESC")
    List<AutomationLog> findRecentByRuleId(@Param("ruleId") UUID ruleId, Pageable pageable);

    Page<AutomationLog> findByStatus(String status, Pageable pageable);

    @Query("SELECT al FROM AutomationLog al WHERE al.createdAt >= :since ORDER BY al.createdAt DESC")
    Page<AutomationLog> findSince(@Param("since") OffsetDateTime since, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AutomationLog al WHERE al.ruleId = :ruleId")
    void deleteAllByRuleId(@Param("ruleId") UUID ruleId);

    @Query("SELECT al FROM AutomationLog al WHERE al.ruleId = :ruleId AND al.status = :status ORDER BY al.createdAt DESC")
    List<AutomationLog> findByRuleIdAndStatus(@Param("ruleId") UUID ruleId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(al) FROM AutomationLog al WHERE al.ruleId = :ruleId AND al.status = :status")
    long countByRuleIdAndStatus(@Param("ruleId") UUID ruleId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM AutomationLog al WHERE al.createdAt < :before")
    int deleteOlderThan(@Param("before") OffsetDateTime before);
}