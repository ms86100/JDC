package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "execution_replay_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionReplaySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "session_start", nullable = false)
    private LocalDateTime sessionStart;

    @Column(name = "session_end")
    private LocalDateTime sessionEnd;

    @Column(name = "playback_position_ms")
    @Builder.Default
    private Integer playbackPositionMs = 0;

    @Column(name = "playback_speed", precision = 3, scale = 2)
    @Builder.Default
    private java.math.BigDecimal playbackSpeed = new java.math.BigDecimal("1.00");

    @Column(name = "is_playing")
    @Builder.Default
    private Boolean isPlaying = false;

    @Column(name = "created_by")
    private UUID createdBy;
}