package com.avionics_systems.version.repository;

import com.avionics_systems.version.entity.VersionDeployment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VersionDeploymentRepository extends JpaRepository<VersionDeployment, UUID> {

    List<VersionDeployment> findByVersionId(UUID versionId);

    Page<VersionDeployment> findByVersionId(UUID versionId, Pageable pageable);

    List<VersionDeployment> findByEnvironmentAndStatus(String environment, String status);

    List<VersionDeployment> findByVersionIdAndEnvironment(UUID versionId, String environment);
}