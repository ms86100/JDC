package com.jira.version.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "version_deployments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "deployment_id", nullable = false, length = 255)
    private String deploymentId;

    @Column(name = "environment", nullable = false, length = 50)
    private String environment;

    @Column(name = "deployment_url", columnDefinition = "TEXT")
    private String deploymentUrl;

    @Column(name = "build_number", length = 100)
    private String buildNumber;

    @Column(name = "build_url", columnDefinition = "TEXT")
    private String buildUrl;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Column(name = "deployed_by")
    private UUID deployedBy;

    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "status", length = 50)
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}