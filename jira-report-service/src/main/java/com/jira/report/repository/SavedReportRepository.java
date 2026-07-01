package com.jira.report.repository;

import com.jira.report.entity.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedReportRepository extends JpaRepository<SavedReport, UUID> {

    List<SavedReport> findByOwnerId(UUID ownerId);

    List<SavedReport> findByOwnerIdAndReportType(UUID ownerId, String reportType);

    List<SavedReport> findByIsSharedTrue();

    List<SavedReport> findByOwnerIdOrIsSharedTrue(UUID ownerId);
}