package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Card color rule (conditional coloring).
 */
@Entity
@Table(name = "board_card_colors", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_card_colors_board", columnList = "board_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCardColor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardConfig boardConfig;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;  // Hex color (e.g., '#ff0000')

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<CardColorCondition> conditions = new ArrayList<>();

    @Column(nullable = false)
    private Integer sequence;

    @Column
    @Builder.Default
    private Boolean enabled = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardColorCondition {
        private String field;      // e.g., "priority", "status", "labels"
        private String operator;   // EQUALS, NOT_EQUALS, CONTAINS, IN, NOT_IN, IS, IS_NOT
        private Object value;      // The value to compare against
    }
}