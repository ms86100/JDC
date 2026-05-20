package com.jira.migration.repository;

import com.jira.migration.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntityStatusRepository extends JpaRepository<EntityStatus, UUID> {

    List<EntityStatus> findByJobIdOrderByProcessingOrderAsc(UUID jobId);

    List<EntityStatus> findByJobIdAndStatus(UUID jobId, String status);

    List<EntityStatus> findByJobIdAndEntityType(UUID jobId, String entityType);

    @Query("SELECT e FROM EntityStatus e WHERE e.jobId = :jobId AND e.status = 'FAILED' ORDER BY e.processingOrder ASC")
    List<EntityStatus> findFailedEntities(@Param("jobId") UUID jobId);

    @Query("SELECT COUNT(e) FROM EntityStatus e WHERE e.jobId = :jobId AND e.status = :status")
    long countByJobIdAndStatus(@Param("jobId") UUID jobId, @Param("status") String status);

    @Modifying
    @Query("UPDATE EntityStatus e SET e.status = :newStatus WHERE e.jobId = :jobId AND e.status = :oldStatus")
    int updateStatusByJobIdAndStatus(@Param("jobId") UUID jobId, @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus);

    @Query("SELECT e.entityType, COUNT(e) FROM EntityStatus e WHERE e.jobId = :jobId GROUP BY e.entityType")
    List<Object[]> countByEntityType(@Param("jobId") UUID jobId);

    Optional<EntityStatus> findByJobIdAndEntityTypeAndSourceIdentifier(@Param("jobId") UUID jobId,
                                                                      @Param("entityType") String entityType,
                                                                      @Param("sourceIdentifier") String sourceIdentifier);

    List<EntityStatus> findByStatus(EntityStatus.Status status);

    void deleteByJobId(UUID jobId);
}