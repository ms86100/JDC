package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVvoRequest {

    @NotNull
    private UUID projectId;

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
    private String milestoneTarget;
    private String specificationReference;
    private UUID assigneeId;
    private Integer storyPoints;
    private List<String> labels;
    private List<UUID> componentIds;
    private List<String> ptsMfclLinks;
    private List<String> nDi;
    private String referenceDocuments;
    private String dtsBaselineVersion;
}
