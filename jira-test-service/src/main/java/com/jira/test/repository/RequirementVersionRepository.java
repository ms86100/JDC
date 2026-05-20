package com.jira.test.repository;

import com.jira.test.entity.RequirementVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementVersionRepository extends JpaRepository<RequirementVersion, UUID> {

    List<RequirementVersion> findByRequirementIdOrderByVersionNumberDesc(UUID requirementId);

    Optional<RequirementVersion> findByRequirementIdAndVersionNumber(UUID requirementId, Integer versionNumber);

    @Query("SELECT MAX(rv.versionNumber) FROM RequirementVersion rv WHERE rv.requirementId = :requirementId")
    Optional<Integer> findMaxVersionByRequirementId(@Param("requirementId") UUID requirementId);
}