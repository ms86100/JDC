package com.avionics_systems.version.repository;

import com.avionics_systems.version.entity.VersionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VersionAuditLogRepository extends JpaRepository<VersionAuditLog, UUID> {

    List<VersionAuditLog> findByVersionIdOrderByCreatedAtDesc(UUID versionId);

    Page<VersionAuditLog> findByVersionId(UUID versionId, Pageable pageable);

    List<VersionAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}