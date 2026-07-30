package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Team availability calendar (time off, vacations, sick days).
 */
@Entity
@Table(name = "team_availability", schema = "jira_plan", indexes = {
    @Index(name = "idx_team_availability_team", columnList = "team_id"),
    @Index(name = "idx_team_availability_user", columnList = "user_id"),
    @Index(name = "idx_team_availability_date", columnList = "date")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private PlanTeam team;

    @Column(name = "user_id")
    private UUID userId;  // NULL means applies to whole team

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 4, scale = 2)
    private java.math.BigDecimal hours;  // Override hours (0 = full day off, 4 = half day)

    @Column(length = 255)
    private String reason;  // e.g., "Vacation", "Conference", "Sick Leave"

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}