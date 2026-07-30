package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_notification_schemes")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectNotificationSchemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "notification_scheme_id", nullable = false)
    private String notificationSchemeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}