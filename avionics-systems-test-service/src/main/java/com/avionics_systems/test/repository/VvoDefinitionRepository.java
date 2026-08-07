package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.VvoDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VvoDefinitionRepository extends JpaRepository<VvoDefinition, UUID> {

    List<VvoDefinition> findByProjectIdAndArchivedFalseOrderByCreatedAtDesc(UUID projectId);

    List<VvoDefinition> findByHlvvoId(UUID hlvvoId);

    List<VvoDefinition> findByFixVersionId(UUID fixVersionId);

    Optional<VvoDefinition> findByIdDoors(String idDoors);

    Optional<VvoDefinition> findByIssueKey(String issueKey);

    List<VvoDefinition> findByProjectIdAndStatusIn(UUID projectId, List<String> statuses);

    boolean existsByIssueKey(String issueKey);

    boolean existsByIdDoors(String idDoors);

    long countByProjectIdAndStatus(UUID projectId, String status);

    long countByProjectId(UUID projectId);

    List<VvoDefinition> findByProjectIdAndArchivedFalse(UUID projectId);

    /**
     * G8: Flag all VVOs whose dts_baseline_version does not match the given version
     * by setting baseline_verified = false.
     */
    @Modifying
    @Query("UPDATE VvoDefinition v SET v.baselineVerified = false " +
           "WHERE v.dtsBaselineVersion IS NOT NULL " +
           "AND v.dtsBaselineVersion <> :dtsVersion " +
           "AND v.archived = false")
    int flagBaselineNotMatching(@Param("dtsVersion") String dtsVersion);
}
