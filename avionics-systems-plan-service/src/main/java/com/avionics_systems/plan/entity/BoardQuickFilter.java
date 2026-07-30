package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quick filter (saved JQL query) for board filtering.
 */
@Entity
@Table(name = "board_quick_filters", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_quick_filters_board", columnList = "board_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardQuickFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardConfig boardConfig;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "filter_query", nullable = false, columnDefinition = "TEXT")
    private String filterQuery;  // JQL query

    @Column(nullable = false)
    private Integer sequence;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(length = 50)
    private String icon;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}