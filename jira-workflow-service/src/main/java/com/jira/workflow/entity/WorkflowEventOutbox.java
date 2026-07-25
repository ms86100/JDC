package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "workflow_event_outbox", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(nullable = false)
    private Boolean published;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "aggregate_type", length = 50)
    private String aggregateType;

    public static final String ISSUE_TRANSITIONED = "ISSUE_TRANSITIONED";
    public static final String ISSUE_CREATED = "ISSUE_CREATED";
    public static final String ISSUE_UPDATED = "ISSUE_UPDATED";
    public static final String ISSUE_DELETED = "ISSUE_DELETED";
    public static final String COMMENT_ADDED = "COMMENT_ADDED";
    public static final String COMMENT_UPDATED = "COMMENT_UPDATED";
    public static final String COMMENT_DELETED = "COMMENT_DELETED";
    public static final String WORKLOG_ADDED = "WORKLOG_ADDED";
    public static final String ATTACHMENT_ADDED = "ATTACHMENT_ADDED";
    public static final String ATTACHMENT_DELETED = "ATTACHMENT_DELETED";
    public static final String VERSION_RELEASED = "VERSION_RELEASED";
    public static final String SPRINT_STARTED = "SPRINT_STARTED";
    public static final String SPRINT_COMPLETED = "SPRINT_COMPLETED";
    public static final String WORKFLOW_PUBLISHED = "WORKFLOW_PUBLISHED";
    public static final String VALIDATOR_FAILED = "VALIDATOR_FAILED";
}
