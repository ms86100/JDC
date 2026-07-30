package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VvoResponse {

    private UUID id;
    private UUID projectId;
    private String issueKey;
    private String summary;
    private String description;
    private String status;
    private UUID hlvvoId;
    private List<String> executionResponsible;
    private List<String> executionDelegation;
    private List<String> vvoUsage;
    private String vvoScope;
    private List<String> testMeanTypeRequested;
    private String operationalConditions;
    private String expectedResults;
    private List<String> realSystemNeeded;
    private List<String> applicability;
    private List<String> supplierApplicability;
    private List<String> associatedRequirements;
    private String idDoors;
    private Integer vvoVersion;
    private UUID cloneSourceId;
    private UUID fixVersionId;
    private String milestoneTarget;
    private String specificationReference;
    private UUID assigneeId;
    private Integer storyPoints;
    private List<String> labels;
    private List<UUID> componentIds;
    private Boolean archived;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
