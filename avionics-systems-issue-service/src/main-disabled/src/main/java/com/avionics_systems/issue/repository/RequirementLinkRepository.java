package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.RequirementLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementLinkRepository extends JpaRepository<RequirementLink, UUID> {

    List<RequirementLink> findByRequirementKey(String requirementKey);

    List<RequirementLink> findByTestIssueId(UUID testIssueId);

    Optional<RequirementLink> findByRequirementKeyAndTestIssueId(String requirementKey, UUID testIssueId);

    @Query("SELECT rl FROM RequirementLink rl WHERE rl.requirementKey = :reqKey AND rl.coverageStatus = :status")
    List<RequirementLink> findByRequirementKeyAndCoverageStatus(@Param("reqKey") String requirementKey, @Param("status") String status);

    @Query("SELECT rl FROM RequirementLink rl WHERE rl.testIssueId IN :testIds")
    List<RequirementLink> findByTestIssueIds(@Param("testIds") List<UUID> testIds);

    @Query("SELECT COUNT(rl) FROM RequirementLink rl WHERE rl.requirementKey = :reqKey")
    Long countByRequirementKey(@Param("reqKey") String requirementKey);

    @Query("SELECT DISTINCT rl.requirementKey FROM RequirementLink rl WHERE rl.testIssueId = :testId")
    List<String> findRequirementKeysByTestId(@Param("testId") UUID testIssueId);

    void deleteByTestIssueId(UUID testIssueId);
}