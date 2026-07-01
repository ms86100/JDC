package com.jira.report.repository;

import com.jira.report.entity.ProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectReportRepository extends JpaRepository<ProjectReport, UUID> {

    List<ProjectReport> findByProjectId(UUID projectId);

    @Query("SELECT p FROM ProjectReport p WHERE p.projectId = :projectId ORDER BY p.reportDate DESC")
    List<ProjectReport> findByProjectIdOrderByDate(@Param("projectId") UUID projectId);

    @Query("SELECT p FROM ProjectReport p WHERE p.projectId = :projectId AND p.reportType = :reportType ORDER BY p.reportDate DESC")
    List<ProjectReport> findByProjectIdAndReportType(
            @Param("projectId") UUID projectId,
            @Param("reportType") String reportType);
}