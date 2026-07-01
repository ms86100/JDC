package com.jira.test.event;

import lombok.Getter;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a test execution is completed
 */
@Getter
public class TestExecutionCompletedEvent extends TestEvent {
    private final UUID executionId;
    private final UUID testId;
    private final String finalStatus;
    private final int passedTests;
    private final int failedTests;
    private final int blockedTests;
    private final int notRunTests;
    private final Map<String, String> defectKeys;

    @Builder
    public TestExecutionCompletedEvent(Object source, UUID projectId, UUID executionId, UUID testId,
                                        String finalStatus, int passedTests, int failedTests,
                                        int blockedTests, int notRunTests, Map<String, String> defectKeys) {
        super(source, projectId);
        this.executionId = executionId;
        this.testId = testId;
        this.finalStatus = finalStatus;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.blockedTests = blockedTests;
        this.notRunTests = notRunTests;
        this.defectKeys = defectKeys;
    }
}