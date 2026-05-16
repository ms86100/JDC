package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_scheme_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSchemeEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "notification_scheme_id", nullable = false)
    private String notificationSchemeId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;  // 'USER', 'GROUP', 'PROJECT_ROLE', 'CURRENT_USER', 'REPORTER', 'ASSIGNEE'

    @Column(name = "notifier_id")
    private String notifierId;  // user_id, group_id, or role_id

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}