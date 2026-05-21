package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CucumberScenario - Parsed BDD scenarios from Cucumber feature files
 */
@Entity
@Table(name = "cucumber_scenarios", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_cs_feature", columnList = "feature_key"),
        @Index(name = "idx_cs_issue", columnList = "issue_id"),
        @Index(name = "idx_cs_tags", columnList = "tags")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CucumberScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_key", nullable = false, length = 255)
    private String featureKey; // Unique: "filename::feature_name"

    @Column(name = "feature_file", nullable = false, length = 500)
    private String featureFile;

    @Column(name = "feature_name", nullable = false, length = 500)
    private String featureName;

    @Column(name = "scenario_name", nullable = false, length = 500)
    private String scenarioName;

    @Column(name = "scenario_key", nullable = false, unique = true, length = 500)
    private String scenarioKey; // Full key: "project::feature::scenario"

    @Column(name = "scenario_type", length = 50)
    @Builder.Default
    private String scenarioType = "Scenario"; // Scenario, Scenario Outline

    @Column(columnDefinition = "TEXT")
    private String background; // Shared background steps

    @Column(columnDefinition = "text[]")
    private String[] tags; // @smoke @regression etc

    // Examples table for Scenario Outline
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, List<String>> examples; // Column name -> values

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "issue_id")
    private UUID issueId; // Linked Jira issue

    @Column(name = "test_set_id")
    private UUID testSetId;

    @CreationTimestamp
    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "import_batch_id")
    private UUID importBatchId;
}