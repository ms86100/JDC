package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.TestVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestVersionRepository extends JpaRepository<TestVersion, UUID> {

    List<TestVersion> findByTestIssueIdOrderByVersionNumberDesc(UUID testIssueId);

    @Query("SELECT MAX(tv.versionNumber) FROM TestVersion tv WHERE tv.testIssueId = :testId")
    Integer findMaxVersionNumber(@Param("testId") UUID testIssueId);

    @Query("SELECT tv FROM TestVersion tv WHERE tv.testIssueId = :testId AND tv.versionNumber = :version")
    Optional<TestVersion> findByTestIssueIdAndVersion(@Param("testId") UUID testIssueId, @Param("version") Integer version);

    @Query("SELECT tv FROM TestVersion tv WHERE tv.testIssueId = :testId ORDER BY tv.versionNumber DESC LIMIT 1")
    Optional<TestVersion> findLatestVersion(@Param("testId") UUID testIssueId);

    void deleteByTestIssueId(UUID testIssueId);
}