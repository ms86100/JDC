package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dev_info_pull_requests", schema = "jira_issue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DevInfoPullRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "issue_id", nullable = false) private UUID issueId;
    @Column(name = "pr_number") private Integer prNumber;
    @Column(length = 500) private String title;
    @Column(length = 30) @Builder.Default private String status = "OPEN";
    @Column(name = "source_branch", length = 500) private String sourceBranch;
    @Column(name = "target_branch", length = 500) private String targetBranch;
    @Column(length = 500) private String repository;
    @Column(columnDefinition = "TEXT") private String url;
    @Column(name = "author_name", length = 200) private String authorName;
    @Column(columnDefinition = "TEXT") private String reviewers;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
