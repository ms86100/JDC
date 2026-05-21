package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DefectLink - Linkage between failed tests and defects
 */
@Entity
@Table(name = "defect_links", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_dl_defect", columnList = "defect_key"),
        @Index(name = "idx_dl_execution", columnList = "test_execution_id"),
        @Index(name = "idx_dl_status", columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "defect_key", nullable = false, length = 100)
    private String defectKey; // Jira issue key like "PROJ-456"

    @Column(name = "defect_summary", columnDefinition = "TEXT")
    private String defectSummary;

    @Column(name = "defect_type", length = 50)
    @Builder.Default
    private String defectType = "BUG";

    @Column(name = "test_execution_id")
    private UUID testExecutionId;

    @Column(name = "step_result_id")
    private UUID stepResultId;

    @Column(name = "test_issue_id")
    private UUID testIssueId;

    @Column(length = 20)
    private String severity; // CRITICAL, MAJOR, MINOR, TRIVIAL

    @Column(length = 30)
    private String status; // OPEN, IN_PROGRESS, REOPENED, CLOSED

    @Column(length = 20)
    private String priority; // P1, P2, P3, P4

    @Column(name = "linked_by")
    private UUID linkedBy;

    @CreationTimestamp
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
}