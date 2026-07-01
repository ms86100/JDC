package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TestVersion - Test version history for audit and rollback
 */
@Entity
@Table(name = "test_versions", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_tv_test", columnList = "test_issue_id"),
        @Index(name = "idx_tv_version", columnList = "version_number")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_issue_id", nullable = false)
    private UUID testIssueId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    // Snapshot of test steps as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_steps", nullable = false, columnDefinition = "jsonb")
    private List<TestStepSnapshot> testSteps;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "changed_by")
    private UUID changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStepSnapshot {
        private Integer stepOrder;
        private String stepType;
        private String description;
        private String testData;
        private String expectedResult;
    }
}