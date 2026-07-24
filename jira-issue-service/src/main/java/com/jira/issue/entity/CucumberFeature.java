package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CucumberFeature - Cucumber feature file metadata
 */
@Entity
@Table(name = "cucumber_features", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_cf_key", columnList = "feature_key")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CucumberFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_key", nullable = false, unique = true, length = 255)
    private String featureKey;

    @Column(name = "feature_file", nullable = false, length = 500)
    private String featureFile;

    @Column(name = "feature_name", nullable = false, length = 500)
    private String featureName;

    @Column(name = "feature_tags", columnDefinition = "text[]")
    private String[] featureTags;

    @Column(columnDefinition = "TEXT")
    private String background;

    @Column(length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "scenario_count")
    @Builder.Default
    private Integer scenarioCount = 0;

    @Column(name = "test_set_id")
    private UUID testSetId;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent; // Full feature file content

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @CreationTimestamp
    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;
}