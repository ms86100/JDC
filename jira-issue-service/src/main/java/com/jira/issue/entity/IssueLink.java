package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_links", schema = "jira_issue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "link_type", nullable = false, length = 50)
    private String linkType;

    @Column(name = "source_issue_id", nullable = false)
    private UUID sourceIssueId;

    @Column(name = "destination_issue_id", nullable = false)
    private UUID destinationIssueId;

    @Column(name = "sequence", nullable = true)
    private Integer sequence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}