package com.jira.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dashboard_shares", schema = "jira_dashboard",
    indexes = {
        @Index(name = "idx_dashboard_share_dashboard_id", columnList = "dashboard_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dashboard_id", nullable = false)
    private UUID dashboardId;

    @Column(name = "share_type", nullable = false, length = 50)
    private String shareType; // USER, GROUP, PROJECT, ROLE, PUBLIC

    @Column(name = "share_id")
    private UUID shareId; // User ID, Group ID, Project ID based on share type

    @Column(name = "share_name", length = 255)
    private String shareName; // Display name for the share

    @Column(name = "permission_type", nullable = false, length = 20)
    @Builder.Default
    private String permissionType = "VIEW"; // VIEW, EDIT

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}