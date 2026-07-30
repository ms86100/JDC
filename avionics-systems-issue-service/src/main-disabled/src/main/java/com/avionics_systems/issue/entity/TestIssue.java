package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TestIssue - Native test case entity extending the Issue table
 * Tests are Avionics Systems issues with issue_type = 'Test' and test-specific fields
 */
@Entity
@Table(name = "issues", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_test_project", columnList = "project_id"),
        @Index(name = "idx_test_status", columnList = "test_status"),
        @Index(name = "idx_test_set", columnList = "test_set_id"),
        @Index(name = "idx_test_type", columnList = "test_type"),
        @Index(name = "idx_test_folder", columnList = "test_repository_folder_id"),
        @Index(name = "idx_test_gherkin", columnList = "gherkin_feature_key, gherkin_scenario_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestIssue {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_key", nullable = false, unique = true)
    private String issueKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Test-specific fields
    @Column(name = "test_type", length = 50)
    @Builder.Default
    private String testType = "MANUAL"; // MANUAL, AUTOMATED, BDD

    @Column(name = "test_status", length = 30)
    @Builder.Default
    private String testStatus = "DRAFT"; // DRAFT, READY, APPROVED, DEPRECATED

    @Column(name = "test_priority", length = 20)
    private String testPriority; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "test_owner_id")
    private UUID testOwnerId;

    // Test Steps stored as JSONB for flexibility
    @Column(name = "test_steps", columnDefinition = "jsonb")
    private String testSteps; // JSON array of test steps

    // Requirement traceability
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "requirement_keys", columnDefinition = "text[]")
    @Builder.Default
    private String[] requirementKeys = new String[]{};

    // Cucumber/BDD integration
    @Column(name = "gherkin_feature_key", length = 255)
    private String gherkinFeatureKey;

    @Column(name = "gherkin_scenario_id", length = 255)
    private String gherkinScenarioId;

    // Organization
    @Column(name = "test_set_id")
    private UUID testSetId;

    @Column(name = "test_plan_id")
    private UUID testPlanId;

    @Column(name = "test_execution_id")
    private UUID testExecutionId; // Last execution

    @Column(name = "test_repository_folder_id")
    private UUID testRepositoryFolderId;

    // Standard Avionics Systems issue fields
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status", nullable = false)
    private IssueStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "priority")
    private IssuePriority priority;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "creator_id")
    private UUID creatorId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "labels", columnDefinition = "text[]")
    @Builder.Default
    private String[] labels = new String[]{};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "component_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] componentIds = new UUID[]{};

    @Column(columnDefinition = "TEXT")
    private String environment;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // Time tracking
    @Column(name = "original_estimate")
    private Long originalEstimate;

    @Column(name = "remaining_estimate")
    private Long remainingEstimate;

    @Column(name = "time_spent")
    private Long timeSpent;

    @Column(name = "story_points")
    private Integer storyPoints;

    // Versions
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "affects_versions", columnDefinition = "uuid[]")
    private UUID[] affectsVersions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fix_versions", columnDefinition = "uuid[]")
    private UUID[] fixVersions;

    // Security
    @Column(name = "security_level_id")
    private UUID securityLevelId;

    // Hierarchy
    @Column(name = "epic_id")
    private UUID epicId;

    @Column(name = "parent_issue_id")
    private UUID parentIssueId;

    // Agile
    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    // Metadata
    @Column(name = "vote_count")
    @Builder.Default
    private Integer voteCount = 0;

    @Column(name = "watcher_count")
    @Builder.Default
    private Integer watcherCount = 0;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    // Helper methods for test steps
    public List<TestStepDto> parseTestSteps() {
        if (testSteps == null || testSteps.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // Simple JSON parsing - in production use ObjectMapper
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStepDto {
        private Integer stepOrder;
        private String stepType; // GIVEN, WHEN, THEN, AND, BUT
        private String description;
        private String testData;
        private String expectedResult;
    }
}