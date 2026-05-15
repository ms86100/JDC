package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_statuses", schema = "jira_issue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String category = "TODO";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}