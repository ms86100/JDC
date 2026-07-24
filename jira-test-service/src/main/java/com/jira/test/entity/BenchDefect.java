package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bench_defect")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchDefect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_key", length = 20)
    private String issueKey;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    @Builder.Default
    private String status = "OPEN";

    @Column(length = 20)
    private String severity;

    @Column(length = 10)
    private String criticality;

    @Column(name = "defect_type", length = 50)
    private String defectType;

    @Column(name = "defect_origin", length = 50)
    private String defectOrigin;

    @Column(name = "defect_impact", length = 50)
    private String defectImpact;

    @Column(name = "defect_impact_rationale", columnDefinition = "TEXT")
    private String defectImpactRationale;

    @Column(name = "ltm_defect_type", length = 50)
    private String ltmDefectType;

    // Origin category (cascading)
    @Column(name = "defect_origin_category_id")
    private UUID defectOriginCategoryId;

    @Column(name = "defect_origin_sub_item_id")
    private UUID defectOriginSubItemId;

    // Detection
    @Column(name = "detected_on_program_id")
    private UUID detectedOnProgramId;

    @Column(name = "detected_on_date")
    private LocalDateTime detectedOnDate;

    @Column(name = "detected_on_test_mean_id")
    private UUID detectedOnTestMeanId;

    // Applicability
    @Column(name = "applicable_to_program_ids", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> applicableToProgramIds = List.of();

    @Column(name = "applicable_to_test_means", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> applicableToTestMeans = List.of();

    @Column(name = "affected_ata", length = 50)
    private String affectedAta;

    // Versions
    @Column(name = "affects_version_id")
    private UUID affectsVersionId;

    @Column(name = "fix_version_id")
    private UUID fixVersionId;

    // Analysis
    @Column(name = "test_configuration", columnDefinition = "TEXT")
    private String testConfiguration;

    @Column(columnDefinition = "TEXT")
    private String workaround;

    @Column(name = "change_reference", length = 255)
    private String changeReference;

    // Dates
    @Column(name = "objective_date_analysis")
    private LocalDate objectiveDateAnalysis;

    @Column(name = "objective_date_closure")
    private LocalDate objectiveDateClosure;

    // Source
    @Column(name = "source_tech_event_id")
    private UUID sourceTechEventId;

    // Assignment
    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(length = 20)
    private String priority;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> labels = List.of();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
