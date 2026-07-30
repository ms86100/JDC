package com.avionics_systems.version.repository;

import com.avionics_systems.version.entity.IssueAffectsVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface IssueAffectsVersionRepository extends JpaRepository<IssueAffectsVersion, UUID> {

    List<IssueAffectsVersion> findByIssueId(UUID issueId);

    List<IssueAffectsVersion> findByVersionId(UUID versionId);

    boolean existsByIssueIdAndVersionId(UUID issueId, UUID versionId);

    @Modifying
    @Query("DELETE FROM IssueAffectsVersion iav WHERE iav.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") UUID issueId);

    @Modifying
    @Query("DELETE FROM IssueAffectsVersion iav WHERE iav.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") UUID versionId);

    @Query("SELECT COUNT(iav) FROM IssueAffectsVersion iav WHERE iav.versionId = :versionId")
    long countByVersionId(@Param("versionId") UUID versionId);
}