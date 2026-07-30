package com.avionics_systems.report.repository;

import com.avionics_systems.report.entity.SprintReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintReportRepository extends JpaRepository<SprintReport, UUID> {

    List<SprintReport> findBySprintId(UUID sprintId);

    List<SprintReport> findByProjectId(UUID projectId);

    Optional<SprintReport> findTopBySprintIdOrderByCreatedAtDesc(UUID sprintId);

    @Query("SELECT s FROM SprintReport s WHERE s.projectId = :projectId ORDER BY s.startDate DESC")
    List<SprintReport> findByProjectIdOrderByDate(@Param("projectId") UUID projectId);
}