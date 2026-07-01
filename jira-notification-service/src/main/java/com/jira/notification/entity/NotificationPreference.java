package com.jira.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences", schema = "jira_notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(NotificationPreferenceId.class)
public class NotificationPreference {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "notification_type", nullable = false, length = 100)
    private String notificationType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}