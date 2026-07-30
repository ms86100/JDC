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
public class ScriptVersionResponse {

    private UUID id;
    private UUID scriptId;
    private Integer version;
    private String scriptBody;
    private String changeSummary;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
