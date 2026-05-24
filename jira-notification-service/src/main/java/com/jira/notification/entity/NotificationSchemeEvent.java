package com.jira.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_scheme_events", schema = "jira_notification",
        uniqueConstraints = @UniqueConstraint(columnNames = {"scheme_id", "event_type", "recipient_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSchemeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scheme_id", nullable = false)
    private UUID schemeId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "recipient_type", nullable = false, length = 50)
    private String recipientType;

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "recipient_group")
    private String recipientGroup;

    @Column(name = "notification_template_id")
    private UUID notificationTemplateId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "notify_assignee")
    @Builder.Default
    private Boolean notifyAssignee = false;

    @Column(name = "notify_reporter")
    @Builder.Default
    private Boolean notifyReporter = false;

    @Column(name = "notify_watchers")
    @Builder.Default
    private Boolean notifyWatchers = false;

    @Column(name = "notify_voters")
    @Builder.Default
    private Boolean notifyVoters = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}