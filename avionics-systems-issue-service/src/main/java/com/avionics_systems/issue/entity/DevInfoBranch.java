package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dev_info_branches", schema = "jira_issue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DevInfoBranch {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "issue_id", nullable = false) private UUID issueId;
    @Column(name = "branch_name", nullable = false, length = 500) private String branchName;
    @Column(length = 500) private String repository;
    @Column(columnDefinition = "TEXT") private String url;
    @Column(length = 30) @Builder.Default private String status = "ACTIVE";
    @Column(name = "created_from_issue") @Builder.Default private Boolean createdFromIssue = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
