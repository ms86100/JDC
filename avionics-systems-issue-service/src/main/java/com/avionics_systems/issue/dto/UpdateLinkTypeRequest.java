package com.avionics_systems.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLinkTypeRequest {
    private String name;
    private String inward;
    private String outward;
    private Boolean isActive;
}