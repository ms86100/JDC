package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sprint_events", schema = "jira_plan", indexes = {
    @Index(name = "idx_sprint_events_sprint", columnList = "sprint_id"),
    @Index(name = "idx_sprint_events_timestamp", columnList = "event_timestamp")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "plan_item_id")
    private UUID planItemId;

    @Column(name = "old_value")
    private Integer oldValue;

    @Column(name = "new_value")
    private Integer newValue;

    @Column(name = "points_delta")
    private Integer pointsDelta;

    @Column(name = "event_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    @Column(name = "user_id")
    private UUID userId;
}
