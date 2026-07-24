package com.jira.test.repository;

import com.jira.test.entity.ProblemReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemReportRepository extends JpaRepository<ProblemReport, UUID> {

    List<ProblemReport> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<ProblemReport> findByLinkedTechEventId(UUID linkedTechEventId);

    List<ProblemReport> findByStatus(String status);

    List<ProblemReport> findByPrType(String prType);

    Optional<ProblemReport> findByIssueKey(String issueKey);

    long countByProjectId(UUID projectId);
}
