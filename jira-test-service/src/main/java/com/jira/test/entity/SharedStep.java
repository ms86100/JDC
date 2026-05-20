package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shared_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "steps", columnDefinition = "JSONB")
    private String steps; // JSON array of steps

    @Column(name = "current_version")
    @Builder.Default
    private Integer currentVersion = 1;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "folder_id")
    private UUID folderId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column
    @Builder.Default
    private Boolean archived = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}