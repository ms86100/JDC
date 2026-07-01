package com.jira.test.event;

import lombok.Getter;
import lombok.Builder;

import java.util.UUID;

/**
 * Event fired when a test run is updated (step results changed, status changed, etc.)
 */
@Getter
public class TestRunUpdatedEvent extends TestEvent {
    private final UUID executionId;
    private final UUID testId;
    private final UUID stepId;
    private final String previousStatus;
    private final String newStatus;
    private final String updatedBy;

    @Builder
    public TestRunUpdatedEvent(Object source, UUID projectId, UUID executionId, UUID testId,
                                UUID stepId, String previousStatus, String newStatus, String updatedBy) {
        super(source, projectId);
        this.executionId = executionId;
        this.testId = testId;
        this.stepId = stepId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.updatedBy = updatedBy;
    }
}