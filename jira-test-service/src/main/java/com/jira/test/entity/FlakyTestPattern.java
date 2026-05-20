package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "flaky_test_patterns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlakyTestPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "pattern_type", nullable = false, length = 100)
    private String patternType; // intermittent, environmental, timing, data-dependent

    @Column(name = "pattern_description", columnDefinition = "TEXT")
    private String patternDescription;

    @Column(name = "frequency_score", precision = 5, scale = 2)
    private java.math.BigDecimal frequencyScore;

    @Column(name = "affected_environments", columnDefinition = "JSONB")
    private String affectedEnvironments; // List of environment IDs

    @Column(name = "affected_builds", columnDefinition = "JSONB")
    private String affectedBuilds; // List of build numbers

    @Column(name = "root_cause_category", length = 100)
    private String rootCauseCategory;

    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private java.math.BigDecimal confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
}