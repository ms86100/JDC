package com.avionics_systems.test.event;

import lombok.Getter;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when coverage metrics are recalculated
 */
@Getter
public class CoverageRecalculatedEvent extends TestEvent {
    private final UUID requirementId;
    private final UUID testPlanId;
    private final double coveragePercentage;
    private final int totalTests;
    private final int coveredTests;
    private final List<UUID> impactedRequirementIds;

    @Builder
    public CoverageRecalculatedEvent(Object source, UUID projectId, UUID requirementId,
                                       UUID testPlanId, double coveragePercentage, int totalTests,
                                       int coveredTests, List<UUID> impactedRequirementIds) {
        super(source, projectId);
        this.requirementId = requirementId;
        this.testPlanId = testPlanId;
        this.coveragePercentage = coveragePercentage;
        this.totalTests = totalTests;
        this.coveredTests = coveredTests;
        this.impactedRequirementIds = impactedRequirementIds;
    }
}