package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "execution_timeline_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // step_start, step_complete, assertion, screenshot, log, error

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "step_index")
    private Integer stepIndex;

    @Column(name = "event_data", columnDefinition = "JSONB")
    private String eventData;

    @Column(name = "screenshot_path", length = 500)
    private String screenshotPath;

    @Column(name = "log_entries", columnDefinition = "JSONB")
    private String logEntries;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;
}