package com.avionics_systems.test.event;

import lombok.Getter;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when tests are imported (Cucumber, JUnit, CI/CD)
 */
@Getter
public class TestImportedEvent extends TestEvent {
    private final UUID batchId;
    private final String importSource;
    private final String importType;
    private final int totalImported;
    private final int successCount;
    private final int failureCount;
    private final List<String> errors;
    private final UUID testPlanId;

    @Builder
    public TestImportedEvent(Object source, UUID projectId, UUID batchId, String importSource,
                             String importType, int totalImported, int successCount, int failureCount,
                             List<String> errors, UUID testPlanId) {
        super(source, projectId);
        this.batchId = batchId;
        this.importSource = importSource;
        this.importType = importType;
        this.totalImported = totalImported;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors;
        this.testPlanId = testPlanId;
    }
}