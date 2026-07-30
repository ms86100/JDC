package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dev_info_commits", schema = "jira_issue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DevInfoCommit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "issue_id", nullable = false) private UUID issueId;
    @Column(name = "commit_hash", nullable = false, length = 100) private String commitHash;
    @Column(columnDefinition = "TEXT") private String message;
    @Column(name = "author_name", length = 200) private String authorName;
    @Column(name = "author_email", length = 300) private String authorEmail;
    @Column(length = 500) private String repository;
    @Column(name = "repository_url", columnDefinition = "TEXT") private String repositoryUrl;
    @Column(columnDefinition = "TEXT") private String url;
    @Column(name = "committed_at") private LocalDateTime committedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
