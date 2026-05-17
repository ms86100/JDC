package com.jira.version.repository;

import com.jira.version.entity.IssueFixVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface IssueFixVersionRepository extends JpaRepository<IssueFixVersion, UUID> {

    List<IssueFixVersion> findByIssueId(UUID issueId);

    List<IssueFixVersion> findByVersionId(UUID versionId);

    boolean existsByIssueIdAndVersionId(UUID issueId, UUID versionId);

    @Modifying
    @Query("DELETE FROM IssueFixVersion ifv WHERE ifv.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") UUID issueId);

    @Modifying
    @Query("DELETE FROM IssueFixVersion ifv WHERE ifv.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") UUID versionId);

    @Query("SELECT COUNT(ifv) FROM IssueFixVersion ifv WHERE ifv.versionId = :versionId")
    long countByVersionId(@Param("versionId") UUID versionId);
}