package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVvoRequest {

    @NotBlank
    private String summary;

    private String description;
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
    private String milestoneTarget;
    private String specificationReference;
    private UUID assigneeId;
    private UUID fixVersionId;
    private Integer storyPoints;
    private List<String> labels;
    private List<UUID> componentIds;
    private String status;
}
