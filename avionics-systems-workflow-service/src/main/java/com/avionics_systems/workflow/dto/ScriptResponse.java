package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptResponse {

    private UUID id;
    private String name;
    private String description;
    private String scriptType;
    private String scriptKey;
    private String scriptBody;
    private String category;
    private Integer version;
    private Boolean isEnabled;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
