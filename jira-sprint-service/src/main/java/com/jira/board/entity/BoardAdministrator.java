package com.jira.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "board_administrators")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardAdministrator {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "board_id", nullable = false) private UUID boardId;
    @Column(name = "holder_id", nullable = false) private UUID holderId;
    @Column(name = "holder_type", nullable = false, length = 20) @Builder.Default private String holderType = "USER";
    @CreationTimestamp @Column(name = "added_at", updatable = false) private LocalDateTime addedAt;
}
