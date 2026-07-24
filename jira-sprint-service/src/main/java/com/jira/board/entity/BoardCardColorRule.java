package com.jira.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "board_card_color_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardCardColorRule {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "board_id", nullable = false) private UUID boardId;
    @Column(name = "color_method", nullable = false, length = 30) private String colorMethod;
    @Column(name = "match_value", columnDefinition = "TEXT") private String matchValue;
    @Column(length = 10) @Builder.Default private String color = "#6c757d";
    @Column(nullable = false) @Builder.Default private Integer position = 0;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
