package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dev_info_builds", schema = "jira_issue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DevInfoBuild {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "issue_id", nullable = false) private UUID issueId;
    @Column(name = "build_number", length = 100) private String buildNumber;
    @Column(name = "plan_key", length = 200) private String planKey;
    @Column(length = 30) @Builder.Default private String status = "IN_PROGRESS";
    @Column(columnDefinition = "TEXT") private String url;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
