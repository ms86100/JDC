package com.jira.version.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "version_build_references")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionBuildReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "build_number", nullable = false, length = 100)
    private String buildNumber;

    @Column(name = "build_url", columnDefinition = "TEXT")
    private String buildUrl;

    @Column(name = "build_status", length = 50)
    private String buildStatus;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "author_email", length = 255)
    private String authorEmail;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}