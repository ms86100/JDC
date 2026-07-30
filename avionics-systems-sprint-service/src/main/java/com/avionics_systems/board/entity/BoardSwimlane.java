package com.avionics_systems.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "board_swimlanes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardSwimlane {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "board_id", nullable = false) private UUID boardId;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "jql_query", columnDefinition = "TEXT") private String jqlQuery;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) @Builder.Default private Integer position = 0;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
