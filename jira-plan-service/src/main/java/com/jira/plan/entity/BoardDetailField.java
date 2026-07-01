package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detail view field configuration (fields shown on card hover/click).
 */
@Entity
@Table(name = "board_detail_fields", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_detail_fields_board", columnList = "board_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDetailField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardConfig boardConfig;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;  // summary, priority, assignee, reporter, labels, etc.

    @Column(name = "field_label", length = 255)
    private String fieldLabel;

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "is_visible")
    @Builder.Default
    private Boolean isVisible = true;

    @Column(name = "field_type", length = 50)
    @Builder.Default
    private String fieldType = "STANDARD";  // STANDARD, CUSTOM, ESCALATION

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}