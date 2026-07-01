package com.jira.version.repository;

import com.jira.version.entity.VersionBuildReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VersionBuildReferenceRepository extends JpaRepository<VersionBuildReference, UUID> {

    List<VersionBuildReference> findByVersionId(UUID versionId);

    List<VersionBuildReference> findByVersionIdOrderByCreatedAtDesc(UUID versionId);

    List<VersionBuildReference> findByBuildNumberContaining(String buildNumber);
}