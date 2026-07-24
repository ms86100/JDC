package com.jira.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gadget_instances", schema = "jira_dashboard",
    indexes = {
        @Index(name = "idx_gadget_instance_dashboard_id", columnList = "dashboard_id"),
        @Index(name = "idx_gadget_instance_gadget_id", columnList = "gadget_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GadgetInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dashboard_id", nullable = false)
    private UUID dashboardId;

    @Column(name = "gadget_id", nullable = false)
    private UUID gadgetId;

    @Column(length = 255)
    private String title;

    @Column(name = "position_row", nullable = false)
    @Builder.Default
    private Integer positionRow = 0;

    @Column(name = "position_column", nullable = false)
    @Builder.Default
    private Integer positionColumn = 0;

    @Column(name = "width", nullable = false)
    @Builder.Default
    private Integer width = 1;

    @Column(name = "height", nullable = false)
    @Builder.Default
    private Integer height = 1;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON configuration specific to this instance

    @Column(columnDefinition = "TEXT")
    private String filters; // JSON filter configurations

    @Column(name = "color", length = 7)
    @Builder.Default
    private String color = "#ffffff";

    @Column(name = "is_minimized", nullable = false)
    @Builder.Default
    private Boolean isMinimized = false;

    @Column(name = "is_collapsed", nullable = false)
    @Builder.Default
    private Boolean isCollapsed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}