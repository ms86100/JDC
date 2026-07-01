package com.jira.migration.repository;

import com.jira.migration.entity.MigrationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<MigrationJob> findByJobStatus(String jobStatus, Pageable pageable);

    List<MigrationJob> findByJobTypeOrderByInitiatedAtDesc(String jobType);

    Page<MigrationJob> findByJobType(String jobType, Pageable pageable);

    List<MigrationJob> findByInitiatedByOrderByInitiatedAtDesc(UUID initiatedBy);

    Page<MigrationJob> findByInitiatedBy(UUID initiatedBy, Pageable pageable);

    @Query("SELECT j FROM MigrationJob j WHERE j.jobStatus IN ('PENDING', 'IN_PROGRESS') ORDER BY j.initiatedAt ASC")
    List<MigrationJob> findPendingJobs();

    @Query("SELECT j FROM MigrationJob j WHERE j.sourceProjectId = :projectId OR j.targetProjectId = :projectId ORDER BY j.initiatedAt DESC")
    List<MigrationJob> findByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT j FROM MigrationJob j WHERE j.canRollback = true AND j.jobStatus = 'COMPLETED' ORDER BY j.initiatedAt DESC")
    List<MigrationJob> findRollbackableJobs();

    @Query("SELECT COUNT(j) FROM MigrationJob j WHERE j.jobStatus = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT j FROM MigrationJob j WHERE j.initiatedBy = :userId AND j.jobStatus = :status")
    Page<MigrationJob> findByUserAndStatus(@Param("userId") UUID userId, @Param("status") String status, Pageable pageable);

    Optional<MigrationJob> findByIdAndInitiatedBy(UUID id, UUID initiatedBy);

    @Query("SELECT j FROM MigrationJob j WHERE j.jobStatus = :status ORDER BY j.initiatedAt DESC")
    Page<MigrationJob> findByStatusOrderByInitiatedAtDesc(@Param("status") String status, Pageable pageable);
}