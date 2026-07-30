package com.avionics_systems.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "swimlane_type", length = 30)
    @Builder.Default
    private String swimlaneType = "NONE";

    @Column(name = "swimlane_field", length = 50)
    @Builder.Default
    private String swimlaneField = "none";

    @Column(name = "collapsed_swimlanes", columnDefinition = "TEXT[]")
    private String[] collapsedSwimlanes;

    @Column(name = "card_color_field", length = 50)
    @Builder.Default
    private String cardColorField = "priority";

    @Column(name = "show_work_vs_capacity")
    @Builder.Default
    private Boolean showWorkVsCapacity = true;

    @Column(name = "default_view", length = 20)
    @Builder.Default
    private String defaultView = "board";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
