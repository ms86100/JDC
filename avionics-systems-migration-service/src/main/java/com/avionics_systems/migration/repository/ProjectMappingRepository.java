package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.ProjectMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMappingRepository extends JpaRepository<ProjectMapping, UUID> {

    List<ProjectMapping> findByJobId(UUID jobId);

    Optional<ProjectMapping> findByJobIdAndSourceKey(UUID jobId, String sourceKey);

    Optional<ProjectMapping> findByJobIdAndTargetId(UUID jobId, UUID targetId);

    Optional<ProjectMapping> findByJobIdAndTargetKey(UUID jobId, String targetKey);

    boolean existsByJobIdAndSourceKey(UUID jobId, String sourceKey);

    boolean existsByJobIdAndTargetKey(UUID jobId, String targetKey);
}