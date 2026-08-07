package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vvm_card_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VvmCardMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "scope", length = 100)
    private String scope;

    @Column(name = "pipeline_status", length = 200)
    private String pipelineStatus;

    @Column(name = "ltr_reference", length = 100)
    private String ltrReference;

    @Column(name = "expert_review_status", length = 20)
    @Builder.Default
    private String expertReviewStatus = "To do";

    @Column(name = "testing_review_status", length = 20)
    @Builder.Default
    private String testingReviewStatus = "To do";

    @Column(name = "safety_review_status", length = 20)
    @Builder.Default
    private String safetyReviewStatus = "To do";

    @Column(name = "validation_count")
    @Builder.Default
    private int validationCount = 0;

    @Column(name = "verification_count")
    @Builder.Default
    private int verificationCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
