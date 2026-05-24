package com.jira.dashboard.repository;

import com.jira.dashboard.entity.Dashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    List<Dashboard> findByOwnerId(UUID ownerId);

    Page<Dashboard> findByOwnerId(UUID ownerId, Pageable pageable);

    List<Dashboard> findByProjectId(UUID projectId);

    Page<Dashboard> findByIsSharedTrue(Pageable pageable);

    List<Dashboard> findByIsFavoriteTrueAndOwnerId(UUID ownerId);

    @Query("SELECT d FROM Dashboard d WHERE d.ownerId = :ownerId OR d.isShared = true ORDER BY d.popularity DESC")
    List<Dashboard> findAccessibleDashboards(@Param("ownerId") UUID ownerId);

    @Query("SELECT d FROM Dashboard d WHERE d.ownerId = :ownerId AND (d.isShared = true OR d.projectId = :projectId)")
    List<Dashboard> findByOwnerIdAndProjectAccess(@Param("ownerId") UUID ownerId, @Param("projectId") UUID projectId);

    @Query("SELECT d FROM Dashboard d WHERE d.isSystem = true ORDER BY d.ordering ASC")
    List<Dashboard> findSystemDashboards();

    @Query("SELECT d FROM Dashboard d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Dashboard> searchByName(@Param("query") String query);
}