package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentResponse {

    private UUID id;
    private UUID projectId;
    private String componentName;
    private String componentPath;
    private String ownershipTeam;
    private String ownershipContact;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}