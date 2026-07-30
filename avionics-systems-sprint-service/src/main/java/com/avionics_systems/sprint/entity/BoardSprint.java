package com.avionics_systems.sprint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** @deprecated Legacy duplicate of {@link com.avionics_systems.board.entity.BoardSprint}; not JPA-mapped. */
@Deprecated
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardSprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "state", nullable = false, length = 50)
    @Builder.Default
    private String state = "ACTIVE";  // ACTIVE, COMPLETED, FUTURE, CLOSED

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "complete_date")
    private LocalDateTime completeDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // States
    public static final String STATE_FUTURE = "FUTURE";
    public static final String STATE_ACTIVE = "ACTIVE";
    public static final String STATE_COMPLETED = "COMPLETED";
    public static final String STATE_CLOSED = "CLOSED";
}