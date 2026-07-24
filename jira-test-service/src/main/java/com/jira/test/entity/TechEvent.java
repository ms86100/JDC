package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tech_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechEvent {

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

    @Column(length = 40)
    @Builder.Default
    private String status = "OPEN";

    // Reporter/Team
    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "reporter_team_id")
    private UUID reporterTeamId;

    @Column(name = "team_for_analysis_id")
    private UUID teamForAnalysisId;

    // Detection context
    @Column(name = "detected_on_program_id")
    private UUID detectedOnProgramId;

    @Column(name = "detected_on_date")
    private LocalDateTime detectedOnDate;

    @Column(name = "detected_on_test_mean_id")
    private UUID detectedOnTestMeanId;

    // Impact
    @Column(name = "impacted_ac_system_id")
    private UUID impactedAcSystemId;

    @Column(name = "impacted_ata_chapter_id")
    private UUID impactedAtaChapterId;

    @Column(name = "impacted_msf", length = 255)
    private String impactedMsf;

    @Column(name = "impacted_function_id")
    private UUID impactedFunctionId;

    @Column(name = "impacted_partition", length = 100)
    private String impactedPartition;

    @Column(name = "system_supplier_id")
    private UUID systemSupplierId;

    // Classification
    @Column(name = "defect_type", length = 50)
    private String defectType;

    @Column(name = "defect_origin", length = 50)
    private String defectOrigin;

    @Column(name = "defect_impact", length = 50)
    private String defectImpact;

    @Column(name = "defect_impact_rationale", columnDefinition = "TEXT")
    private String defectImpactRationale;

    // Versions
    @Column(name = "affects_version_id")
    private UUID affectsVersionId;

    @Column(name = "fix_version_id")
    private UUID fixVersionId;

    // Program applicability
    @Column(name = "applicable_to_program_ids", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> applicableToProgramIds = List.of();

    // Analysis
    @Column(name = "public_analysis", columnDefinition = "TEXT")
    private String publicAnalysis;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "test_configuration", columnDefinition = "TEXT")
    private String testConfiguration;

    @Column(name = "recording_reference", length = 255)
    private String recordingReference;

    @Column(name = "operational_impact", columnDefinition = "TEXT")
    private String operationalImpact;

    @Column(name = "requirement_impact", columnDefinition = "TEXT")
    private String requirementImpact;

    @Column(columnDefinition = "TEXT")
    private String workaround;

    // Rejection
    @Column(name = "rejection_rationale", columnDefinition = "TEXT")
    private String rejectionRationale;

    @Column(name = "rejection_type", length = 50)
    private String rejectionType;

    // Supplier sync
    @Column(name = "supplier_analysis", columnDefinition = "TEXT")
    private String supplierAnalysis;

    @Column(name = "supplier_response", length = 50)
    private String supplierResponse;

    @Column(name = "supplier_status", length = 100)
    private String supplierStatus;

    @Column(name = "final_airbus_response", columnDefinition = "TEXT")
    private String finalAirbusResponse;

    @Column(name = "supplier_sync_project_id")
    private UUID supplierSyncProjectId;

    @Column(name = "supplier_sync_issue_id")
    private UUID supplierSyncIssueId;

    // Linked items
    @Column(name = "linked_change_card_id")
    private UUID linkedChangeCardId;

    @Column(name = "linked_problem_report_id")
    private UUID linkedProblemReportId;

    // Assignment
    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(length = 20)
    private String priority;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> labels = List.of();

    @Column(name = "vv_activity", length = 50)
    private String vvActivity;

    @Column(name = "detected_by", length = 100)
    private String detectedBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
