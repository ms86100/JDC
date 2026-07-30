package com.avionics_systems.issue.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Durable issue change events for downstream consumers (search, boards, notifications).
 */
@Entity
@Table(name = "issue_event_outbox", schema = "jira_issue")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueEventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
