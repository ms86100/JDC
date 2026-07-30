package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RequirementLink - Traceability mapping between requirements and tests
 */
@Entity
@Table(name = "requirement_links", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_rl_req", columnList = "requirement_key"),
        @Index(name = "idx_rl_test", columnList = "test_issue_id"),
        @Index(name = "idx_rl_status", columnList = "coverage_status")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requirement_key", nullable = false, length = 255)
    private String requirementKey; // Avionics Systems issue key like "PROJ-123"

    @Column(name = "requirement_summary", columnDefinition = "TEXT")
    private String requirementSummary;

    @Column(name = "requirement_type", length = 50)
    private String requirementType; // EPIC, STORY, REQUIREMENT, BUG

    @Column(name = "test_issue_id", nullable = false)
    private UUID testIssueId;

    @Column(name = "coverage_status", length = 20)
    @Builder.Default
    private String coverageStatus = "COVERED"; // COVERED, PARTIAL, NOT_COVERED

    @Column(name = "last_test_execution_id")
    private UUID lastTestExecutionId;

    @Column(name = "last_execution_status", length = 30)
    private String lastExecutionStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}