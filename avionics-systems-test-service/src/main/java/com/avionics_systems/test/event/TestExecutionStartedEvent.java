package com.avionics_systems.test.event;

import lombok.Getter;
import lombok.Builder;

import java.util.UUID;

/**
 * Event fired when a test execution is started
 */
@Getter
public class TestExecutionStartedEvent extends TestEvent {
    private final UUID executionId;
    private final UUID testId;
    private final UUID testPlanId;
    private final UUID testSetId;
    private final UUID testerId;
    private final String testEnv;

    @Builder
    public TestExecutionStartedEvent(Object source, UUID projectId, UUID executionId, UUID testId,
                                      UUID testPlanId, UUID testSetId, UUID testerId, String testEnv) {
        super(source, projectId);
        this.executionId = executionId;
        this.testId = testId;
        this.testPlanId = testPlanId;
        this.testSetId = testSetId;
        this.testerId = testerId;
        this.testEnv = testEnv;
    }
}