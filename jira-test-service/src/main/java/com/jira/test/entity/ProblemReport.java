package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "problem_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemReport {

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

    @Column(name = "pr_origin", length = 30)
    private String prOrigin;

    @Column(name = "pr_type", length = 50)
    private String prType;

    @Column(name = "pr_type_rationale", columnDefinition = "TEXT")
    private String prTypeRationale;

    @Column(name = "potential_effects", columnDefinition = "TEXT")
    private String potentialEffects;

    @Column(name = "justification_mitigation", columnDefinition = "TEXT")
    private String justificationMitigation;

    // Detection
    @Column(name = "detected_on_program_id")
    private UUID detectedOnProgramId;

    @Column(name = "detected_on_ac_system_id")
    private UUID detectedOnAcSystemId;

    @Column(name = "applicable_to_program_ids", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> applicableToProgramIds = List.of();

    // Rejection
    @Column(name = "rejection_type", length = 50)
    private String rejectionType;

    @Column(name = "rejection_rationale", columnDefinition = "TEXT")
    private String rejectionRationale;

    // Linked
    @Column(name = "linked_tech_event_id")
    private UUID linkedTechEventId;

    // Versions
    @Column(name = "affects_version_id")
    private UUID affectsVersionId;

    @Column(name = "fix_version_id")
    private UUID fixVersionId;

    // Classification
    @Column(length = 50)
    private String classification;

    // Assignment
    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "system_supplier_id")
    private UUID systemSupplierId;

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
