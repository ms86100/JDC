package com.avionics_systems.test.entity;

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

    @Column(name = "version", nullable = false, length = 50)
    private String version; // Semantic version like "1.0.0", "1.1.0"

    @Column(name = "version_number")
    private Integer versionNumber; // Numeric version for ordering and comparison

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RequirementVersionStatus status = RequirementVersionStatus.DRAFT;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // Full requirement content

    @Column(name = "changelog", columnDefinition = "TEXT")
    private String changelog; // Description of changes from previous version

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "previous_version_id")
    private UUID previousVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_magnitude", length = 20)
    @Builder.Default
    private ChangeMagnitude changeMagnitude = ChangeMagnitude.MINOR;

    @Column(name = "title_snapshot", length = 500)
    private String titleSnapshot;

    @Column(name = "description_snapshot", columnDefinition = "TEXT")
    private String descriptionSnapshot;

    @Column(name = "acceptance_criteria_snapshot", columnDefinition = "JSONB")
    private String acceptanceCriteriaSnapshot;

    @Column(name = "linked_tests_snapshot", columnDefinition = "JSONB")
    private String linkedTestsSnapshot;

    @Column(name = "changed_by")
    private UUID changedBy;

    public enum RequirementVersionStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }

    public enum ChangeMagnitude {
        MINOR, MAJOR, CRITICAL
    }
}