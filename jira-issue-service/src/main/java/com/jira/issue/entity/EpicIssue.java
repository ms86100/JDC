package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "epic_issues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpicIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "epic_id", nullable = false)
    private String epicId;

    @Column(name = "issue_id", nullable = false)
    private String issueId;

    @Column(name = "added_at")
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(name = "added_by")
    private String addedBy;
}