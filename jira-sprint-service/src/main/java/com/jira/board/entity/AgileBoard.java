package com.jira.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agile_boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgileBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "board_type", nullable = false, length = 50)
    @Builder.Default
    private String boardType = "SCRUM";

    @Column(name = "filter_id")
    private UUID filterId;

    @Column(name = "jql_query", columnDefinition = "TEXT")
    private String jqlQuery;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "allow_all_issues", nullable = false)
    @Builder.Default
    private Boolean allowAllIssues = true;

    @Column(name = "card_layout", length = 50)
    @Builder.Default
    private String cardLayout = "FULL";

    @Column(name = "estimation_statistic", length = 100)
    private String estimationStatistic;

    @Column(name = "days_on_board", nullable = false)
    @Builder.Default
    private Integer daysOnBoard = 5;

    @Column(name = "last_viewed")
    private LocalDateTime lastViewed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static final String TYPE_SCRUM = "SCRUM";
    public static final String TYPE_KANBAN = "KANBAN";
    public static final String TYPE_BADGE = "BADGE";

    public static final String LAYOUT_FULL = "FULL";
    public static final String LAYOUT_COMPACT = "COMPACT";
    public static final String LAYOUT_MINI = "MINI";
}