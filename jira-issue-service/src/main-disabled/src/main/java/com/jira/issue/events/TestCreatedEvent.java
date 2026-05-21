package com.jira.issue.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Test Created Event
 * Phase 15 - Event-Driven Architecture
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCreatedEvent {
    private UUID eventId;
    private UUID testId;
    private String issueKey;
    private UUID projectId;
    private String testType;
    private String testStatus;
    private List<String> labels;
    private UUID createdBy;
    private LocalDateTime createdAt;
}