package com.jira.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "board_card_fields")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardCardField {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "board_id", nullable = false) private UUID boardId;
    @Column(name = "field_id", nullable = false, length = 100) private String fieldId;
    @Column(nullable = false) private Integer position;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
