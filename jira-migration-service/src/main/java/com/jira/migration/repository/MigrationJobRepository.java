package com.jira.migration.repository;

import com.jira.migration.entity.MigrationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MigrationJobRepository extends JpaRepository<MigrationJob, UUID> {

    List<MigrationJob> findByJobStatusOrderByInitiatedAtDesc(String jobStatus);

    List<MigrationJob> findByJobTypeOrderByInitiatedAtDesc(String jobType);

    List<MigrationJob> findByInitiatedByOrderByInitiatedAtDesc(UUID initiatedBy);

    @Query("SELECT j FROM MigrationJob j WHERE j.jobStatus IN ('PENDING', 'IN_PROGRESS') ORDER BY j.initiatedAt ASC")
    List<MigrationJob> findPendingJobs();

    @Query("SELECT j FROM MigrationJob j WHERE j.sourceProjectId = :projectId OR j.targetProjectId = :projectId ORDER BY j.initiatedAt DESC")
    List<MigrationJob> findByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT j FROM MigrationJob j WHERE j.canRollback = true AND j.jobStatus = 'COMPLETED' ORDER BY j.initiatedAt DESC")
    List<MigrationJob> findRollbackableJobs();

    @Query("SELECT COUNT(j) FROM MigrationJob j WHERE j.jobStatus = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT j FROM MigrationJob j WHERE j.initiatedBy = :userId AND j.jobStatus = :status")
    List<MigrationJob> findByUserAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    Optional<MigrationJob> findByIdAndInitiatedBy(UUID id, UUID initiatedBy);
}