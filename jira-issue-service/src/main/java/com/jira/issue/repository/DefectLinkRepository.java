package com.jira.issue.repository;

import com.jira.issue.entity.DefectLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DefectLinkRepository extends JpaRepository<DefectLink, UUID> {

    List<DefectLink> findByDefectKey(String defectKey);

    List<DefectLink> findByTestExecutionId(UUID testExecutionId);

    List<DefectLink> findByTestIssueId(UUID testIssueId);

    Optional<DefectLink> findByDefectKeyAndTestExecutionId(String defectKey, UUID testExecutionId);

    @Query("SELECT dl FROM DefectLink dl WHERE dl.defectKey = :defectKey AND dl.status = :status")
    List<DefectLink> findByDefectKeyAndStatus(@Param("defectKey") String defectKey, @Param("status") String status);

    @Query("SELECT COUNT(dl) FROM DefectLink dl WHERE dl.testExecutionId = :execId")
    Long countByExecutionId(@Param("execId") UUID testExecutionId);

    @Query("SELECT dl FROM DefectLink dl WHERE dl.severity = :severity AND dl.status = 'OPEN'")
    List<DefectLink> findOpenDefectsBySeverity(@Param("severity") String severity);

    void deleteByTestExecutionId(UUID testExecutionId);

    void deleteByTestIssueId(UUID testIssueId);
}