package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
    List<AuditLogEntity> findByUserNameOrderByTimestampDesc(String userName);
    List<AuditLogEntity> findByEntityTypeOrderByTimestampDesc(String entityType);

    Page<AuditLogEntity> findAll(Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.timestamp BETWEEN :start AND :end")
    Page<AuditLogEntity> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.timestamp > :start")
    List<AuditLogEntity> findByTimestampAfter(@Param("start") LocalDateTime start);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.userId = :userId")
    Page<AuditLogEntity> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.category = :category")
    Page<AuditLogEntity> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.entityType = :entityType AND a.entityId = :entityId")
    Page<AuditLogEntity> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") String entityId, Pageable pageable);
}