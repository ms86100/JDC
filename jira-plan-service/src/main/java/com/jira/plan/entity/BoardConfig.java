package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Board/RapidView configuration.
 * Main board entity that holds all board-specific settings.
 */
@Entity
@Table(name = "board_configs", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_configs_plan", columnList = "plan_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "board_type", nullable = false, length = 20)
    private String boardType;  // SCRUM, KANBAN, SUSPEND

    @Column(name = "column_config_mode", length = 20)
    @Builder.Default
    private String columnConfigMode = "DEFAULT";

    @Column(name = "constraint_source", length = 50)
    private String constraintSource;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "card_layout_mode", length = 20)
    @Builder.Default
    private String cardLayoutMode = "COMPACT";

    @Column(name = "default_swimlane", length = 50)
    @Builder.Default
    private String defaultSwimlane = "NONE";

    @OneToMany(mappedBy = "boardConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<BoardColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "boardConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<BoardQuickFilter> quickFilters = new ArrayList<>();

    @OneToMany(mappedBy = "boardConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<BoardSwimlane> swimlanes = new ArrayList<>();

    @OneToMany(mappedBy = "boardConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<BoardCardColor> cardColors = new ArrayList<>();

    @OneToMany(mappedBy = "boardConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<BoardDetailField> detailFields = new ArrayList<>();

    @OneToMany(mappedBy = "boardConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<BoardCardLayoutField> cardLayoutFields = new ArrayList<>();

    @Column(name = "feature_sprints")
    @Builder.Default
    private Boolean featureSprints = true;

    @Column(name = "feature_backlog")
    @Builder.Default
    private Boolean featureBacklog = true;

    @Column(name = "feature_estimation")
    @Builder.Default
    private Boolean featureEstimation = true;

    @Column(name = "feature_parallel_sprints")
    @Builder.Default
    private Boolean featureParallelSprints = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addColumn(BoardColumn column) {
        columns.add(column);
        column.setBoardConfig(this);
    }

    public void removeColumn(BoardColumn column) {
        columns.remove(column);
        column.setBoardConfig(null);
    }

    public void addQuickFilter(BoardQuickFilter filter) {
        quickFilters.add(filter);
        filter.setBoardConfig(this);
    }

    public void addSwimlane(BoardSwimlane swimlane) {
        swimlanes.add(swimlane);
        swimlane.setBoardConfig(this);
    }

    public void addCardColor(BoardCardColor cardColor) {
        cardColors.add(cardColor);
        cardColor.setBoardConfig(this);
    }
}