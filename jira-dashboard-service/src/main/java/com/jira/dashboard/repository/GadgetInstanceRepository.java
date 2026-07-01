package com.jira.dashboard.repository;

import com.jira.dashboard.entity.GadgetInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GadgetInstanceRepository extends JpaRepository<GadgetInstance, UUID> {

    List<GadgetInstance> findByDashboardIdOrderByPositionRowAscPositionColumnAsc(UUID dashboardId);

    List<GadgetInstance> findByGadgetId(UUID gadgetId);

    @Modifying
    @Query("DELETE FROM GadgetInstance gi WHERE gi.dashboardId = :dashboardId")
    void deleteByDashboardId(@Param("dashboardId") UUID dashboardId);

    @Query("SELECT COUNT(gi) FROM GadgetInstance gi WHERE gi.dashboardId = :dashboardId")
    long countByDashboardId(@Param("dashboardId") UUID dashboardId);
}