package com.avionics_systems.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "board_issue_detail_fields")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardIssueDetailField {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "board_id", nullable = false) private UUID boardId;
    @Column(name = "field_id", nullable = false, length = 100) private String fieldId;
    @Column(name = "field_group", length = 30) @Builder.Default private String fieldGroup = "GENERAL";
    @Column(nullable = false) @Builder.Default private Integer position = 0;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
