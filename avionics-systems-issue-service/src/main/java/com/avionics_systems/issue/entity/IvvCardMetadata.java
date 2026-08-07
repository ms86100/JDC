package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ivv_card_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IvvCardMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "vvm_card_id")
    private UUID vvmCardId;

    @Column(name = "ivv_type", length = 20, nullable = false)
    @Builder.Default
    private String ivvType = "VALIDATION";

    @Column(name = "requirement_impact", length = 100)
    private String requirementImpact;

    @Column(name = "level", length = 10)
    private String level;

    @Column(name = "statement", columnDefinition = "TEXT")
    private String statement;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    @Column(name = "change_tag", length = 50)
    private String changeTag;

    @Column(name = "change_rationale", columnDefinition = "TEXT")
    private String changeRationale;

    @Column(name = "partition_name", length = 100)
    private String partitionName;

    @Column(name = "product", columnDefinition = "TEXT")
    private String product;

    @Column(name = "equivalence", columnDefinition = "TEXT")
    private String equivalence;

    @Column(name = "category_level")
    @Builder.Default
    private int categoryLevel = 0;

    @Column(name = "mvv")
    @Builder.Default
    private int mvv = 0;

    @Column(name = "vvo_reference", length = 100)
    private String vvoReference;

    @Column(name = "sha", length = 100)
    private String sha;

    @Column(name = "ivv_priority", length = 10)
    private String ivvPriority;

    @Column(name = "test_case_impact", length = 100)
    private String testCaseImpact;

    @Column(name = "test_procedure_impact", length = 100)
    private String testProcedureImpact;

    @Column(name = "evidence", length = 100)
    private String evidence;

    @Column(name = "tests_status", length = 20)
    @Builder.Default
    private String testsStatus = "UNCOVERED";

    @Column(name = "ac_variant", length = 100)
    private String acVariant;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
