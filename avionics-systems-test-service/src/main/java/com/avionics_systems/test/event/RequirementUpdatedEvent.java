package com.avionics_systems.test.event;

import lombok.Getter;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Event fired when a requirement is updated (coverage status changes)
 */
@Getter
public class RequirementUpdatedEvent extends TestEvent {
    private final UUID requirementId;
    private final String requirementKey;
    private final String changeType;
    private final String previousValue;
    private final String newValue;
    private final List<UUID> affectedTestIds;

    @Builder
    public RequirementUpdatedEvent(Object source, UUID projectId, UUID requirementId,
                                    String requirementKey, String changeType, String previousValue,
                                    String newValue, List<UUID> affectedTestIds) {
        super(source, projectId);
        this.requirementId = requirementId;
        this.requirementKey = requirementKey;
        this.changeType = changeType;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.affectedTestIds = affectedTestIds;
    }
}