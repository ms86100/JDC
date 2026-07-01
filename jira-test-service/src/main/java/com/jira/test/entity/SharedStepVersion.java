package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shared_step_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shared_step_id", nullable = false)
    private UUID sharedStepId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "steps", columnDefinition = "JSONB", nullable = false)
    private String steps; // JSON array of step objects

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}