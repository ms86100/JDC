package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.RequirementVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementVersionRepository extends JpaRepository<RequirementVersion, UUID> {

    List<RequirementVersion> findByRequirementIdOrderByCreatedAtDesc(UUID requirementId);

    List<RequirementVersion> findByRequirementIdOrderByVersionNumberDesc(UUID requirementId);

    Optional<RequirementVersion> findByRequirementIdAndVersionNumber(UUID requirementId, Integer versionNumber);

    Optional<RequirementVersion> findFirstByRequirementIdOrderByCreatedAtDesc(UUID requirementId);

    Optional<RequirementVersion> findFirstByRequirementIdAndStatusOrderByCreatedAtDesc(
            UUID requirementId, RequirementVersion.RequirementVersionStatus status);

    List<RequirementVersion> findByRequirementIdAndStatus(UUID requirementId, RequirementVersion.RequirementVersionStatus status);

    @Query("SELECT MAX(rv.versionNumber) FROM RequirementVersion rv WHERE rv.requirementId = :requirementId")
    Optional<Integer> findMaxVersionByRequirementId(@Param("requirementId") UUID requirementId);

    List<RequirementVersion> findByRequirementIdAndVersionNumberLessThanOrderByVersionNumberDesc(UUID requirementId, Integer versionNumber);

    @Query("SELECT rv FROM RequirementVersion rv WHERE rv.requirementId = :requirementId AND rv.versionNumber < :version ORDER BY rv.versionNumber DESC LIMIT 1")
    Optional<RequirementVersion> findPreviousVersion(@Param("requirementId") UUID requirementId, @Param("version") Integer version);

    @Query("SELECT rv FROM RequirementVersion rv WHERE rv.requirementId = :requirementId AND rv.versionNumber > :version ORDER BY rv.versionNumber ASC LIMIT 1")
    Optional<RequirementVersion> findNextVersion(@Param("requirementId") UUID requirementId, @Param("version") Integer version);

    Long countByRequirementId(UUID requirementId);

    List<RequirementVersion> findByRequirementIdAndStatusIn(UUID requirementId, List<RequirementVersion.RequirementVersionStatus> statuses);
}