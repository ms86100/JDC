package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "requirement_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "title_snapshot", length = 500)
    private String titleSnapshot;

    @Column(name = "description_snapshot", columnDefinition = "TEXT")
    private String descriptionSnapshot;

    @Column(name = "acceptance_criteria_snapshot", columnDefinition = "JSONB")
    private String acceptanceCriteriaSnapshot; // [{criteria, status}]

    @Column(name = "linked_tests_snapshot", columnDefinition = "JSONB")
    private String linkedTestsSnapshot; // [{testId, testKey, status}]

    @Column(name = "change_type", length = 50)
    private String changeType; // major, minor, cosmetic

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    @Column(name = "changed_by")
    private UUID changedBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}