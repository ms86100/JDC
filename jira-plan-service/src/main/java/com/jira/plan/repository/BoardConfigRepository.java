package com.jira.plan.repository;

import com.jira.plan.entity.BoardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardConfigRepository extends JpaRepository<BoardConfig, UUID> {

    List<BoardConfig> findByPlanId(UUID planId);

    List<BoardConfig> findByPlanIdAndIsEnabledTrue(UUID planId);

    @Query("SELECT bc FROM BoardConfig bc WHERE bc.plan.id = :planId AND bc.isEnabled = true")
    List<BoardConfig> findEnabledByPlanId(@Param("planId") UUID planId);

    @Query("SELECT COUNT(bc) FROM BoardConfig bc WHERE bc.plan.id = :planId")
    long countByPlanId(@Param("planId") UUID planId);
}