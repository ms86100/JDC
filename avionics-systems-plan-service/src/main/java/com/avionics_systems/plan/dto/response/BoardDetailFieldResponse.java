package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDetailFieldResponse {
    private UUID id;
    private String fieldKey;
    private String fieldLabel;
    private Integer sequence;
    private Boolean isVisible;
    private String fieldType;
}