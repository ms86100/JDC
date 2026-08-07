package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exploratory_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExploratorySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(columnDefinition = "TEXT")
    private String charter;

    @Column(name = "charter_goal", length = 500)
    private String charterGoal;

    @Column(name = "session_type", length = 30)
    @Builder.Default
    private String sessionType = "CHARTER_BASED";

    @Column(name = "time_box_minutes")
    @Builder.Default
    private Integer timeBoxMinutes = 60;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(length = 30)
    @Builder.Default
    private String status = "PLANNED";

    @Column(name = "tester_id")
    private UUID testerId;

    @Column(length = 200)
    private String environment;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> bugs = List.of();

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> ideas = List.of();

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> questions = List.of();

    @Column(name = "evidence_links", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> evidenceLinks = List.of();

    @Column(name = "defect_keys", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> defectKeys = List.of();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
