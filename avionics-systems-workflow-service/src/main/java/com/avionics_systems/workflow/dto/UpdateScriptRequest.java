package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScriptRequest {

    private String name;
    private String description;
    private String scriptBody;
    private Boolean isEnabled;
    private String changeSummary;
}
