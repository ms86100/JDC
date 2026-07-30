package com.avionics_systems.test.event;

import lombok.Getter;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a defect is linked to a test or test step
 */
@Getter
public class DefectLinkedEvent extends TestEvent {
    private final UUID executionId;
    private final UUID stepResultId;
    private final String defectKey;
    private final String severity;
    private final String linkedBy;
    private final List<String> affectedTestIds;

    @Builder
    public DefectLinkedEvent(Object source, UUID projectId, UUID executionId, UUID stepResultId,
                              String defectKey, String severity, String linkedBy,
                              List<String> affectedTestIds) {
        super(source, projectId);
        this.executionId = executionId;
        this.stepResultId = stepResultId;
        this.defectKey = defectKey;
        this.severity = severity;
        this.linkedBy = linkedBy;
        this.affectedTestIds = affectedTestIds;
    }
}