package com.avionics_systems.version.repository;

import com.avionics_systems.version.entity.VersionMetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionMetricSnapshotRepository extends JpaRepository<VersionMetricSnapshot, UUID> {

    List<VersionMetricSnapshot> findByVersionIdOrderBySnapshotDateAsc(UUID versionId);

    Optional<VersionMetricSnapshot> findByVersionIdAndSnapshotDate(UUID versionId, LocalDate snapshotDate);

    @Query("SELECT vms FROM VersionMetricSnapshot vms WHERE vms.versionId = :versionId ORDER BY vms.snapshotDate DESC LIMIT 1")
    Optional<VersionMetricSnapshot> findLatestByVersionId(@Param("versionId") UUID versionId);
}