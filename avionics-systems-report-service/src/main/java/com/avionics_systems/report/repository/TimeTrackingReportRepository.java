package com.avionics_systems.report.repository;

import com.avionics_systems.report.entity.TimeTrackingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimeTrackingReportRepository extends JpaRepository<TimeTrackingReport, UUID> {

    List<TimeTrackingReport> findByUserId(UUID userId);

    List<TimeTrackingReport> findByProjectId(UUID projectId);

    @Query("SELECT t FROM TimeTrackingReport t WHERE t.userId = :userId AND t.startDate >= :startDate AND t.endDate <= :endDate")
    List<TimeTrackingReport> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT t FROM TimeTrackingReport t WHERE t.projectId = :projectId AND t.startDate >= :startDate AND t.endDate <= :endDate")
    List<TimeTrackingReport> findByProjectIdAndDateRange(
            @Param("projectId") UUID projectId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}