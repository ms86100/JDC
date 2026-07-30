package com.avionics_systems.sprint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SprintStatus status;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "created_by")
    private UUID createdBy;

    // Agile board enhancements
    @Column(name = "board_id")
    private UUID boardId;  // Reference to agile board

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "complete_date")
    private LocalDate completeDate;

    @Column(name = "auto_start", nullable = false)
    @Builder.Default
    private Boolean autoStart = false;

    @Column(name = "auto_complete", nullable = false)
    @Builder.Default
    private Boolean autoComplete = false;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "velocity_point_avg")
    private Double velocityPointAvg;  // Rolling average velocity

    @Column(name = "capacity")
    private Integer capacity;  // Sprint capacity (story points or issue count)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SprintStatus {
        CLOSED,
        ACTIVE,
        PLANNING,
        FUTURE,
        COMPLETED
    }
}
