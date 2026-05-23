package com.jira.migration.entity.field;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "board_card_layout_fields", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCardLayoutFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "field_key", nullable = false, length = 255)
    private String fieldKey;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(length = 20)
    @Builder.Default
    private String position = "BOTTOM";

    @Column(name = "visible")
    @Builder.Default
    private Boolean visible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
