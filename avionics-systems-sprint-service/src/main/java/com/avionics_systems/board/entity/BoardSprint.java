package com.avionics_systems.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_sprints")
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

    @Column(nullable = false)
    @Builder.Default
    private int sequence = 0;

    @Column(length = 20)
    @Builder.Default
    private String state = STATE_FUTURE;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "complete_date")
    private LocalDateTime completeDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static final String STATE_FUTURE = "FUTURE";
    public static final String STATE_ACTIVE = "ACTIVE";
    public static final String STATE_COMPLETED = "COMPLETED";
    public static final String STATE_CLOSED = "CLOSED";
}