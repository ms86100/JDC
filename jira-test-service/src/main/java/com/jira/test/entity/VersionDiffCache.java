package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "version_diff_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionDiffCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // test, requirement, dataset, shared_step

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "version_a", nullable = false)
    private Integer versionA;

    @Column(name = "version_b", nullable = false)
    private Integer versionB;

    @Column(name = "diff_data", columnDefinition = "JSONB", nullable = false)
    private String diffData;

    @CreationTimestamp
    @Column(name = "computed_at")
    private LocalDateTime computedAt;
}