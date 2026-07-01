package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_favorites", schema = "jira_plan")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BoardFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}