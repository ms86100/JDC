package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepDependencyResponse {

    private UUID id;
    private UUID parentSharedStepId;
    private String parentName;
    private UUID childSharedStepId;
    private String childName;
    private String dependencyType;
    private Integer depth;
    private Boolean hasCircularDependency;
}