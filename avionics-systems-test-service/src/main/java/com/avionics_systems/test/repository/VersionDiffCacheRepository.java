package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.VersionDiffCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionDiffCacheRepository extends JpaRepository<VersionDiffCache, UUID> {

    Optional<VersionDiffCache> findByEntityTypeAndEntityIdAndVersionAAndVersionB(
            String entityType, UUID entityId, Integer versionA, Integer versionB);
}