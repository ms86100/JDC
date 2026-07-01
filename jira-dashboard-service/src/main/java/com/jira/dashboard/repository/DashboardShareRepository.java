package com.jira.dashboard.repository;

import com.jira.dashboard.entity.DashboardShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DashboardShareRepository extends JpaRepository<DashboardShare, UUID> {

    List<DashboardShare> findByDashboardId(UUID dashboardId);

    List<DashboardShare> findByDashboardIdAndShareType(UUID dashboardId, String shareType);

    @Query("SELECT ds FROM DashboardShare ds WHERE ds.shareId = :shareId AND ds.shareType = :shareType")
    List<DashboardShare> findByShareIdAndShareType(@Param("shareId") UUID shareId, @Param("shareType") String shareType);

    @Modifying
    @Query("DELETE FROM DashboardShare ds WHERE ds.dashboardId = :dashboardId")
    void deleteByDashboardId(@Param("dashboardId") UUID dashboardId);

    boolean existsByDashboardIdAndShareId(UUID dashboardId, UUID shareId);
}