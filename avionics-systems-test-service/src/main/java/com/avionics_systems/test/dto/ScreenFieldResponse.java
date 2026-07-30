package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenFieldResponse {

    private UUID id;
    private UUID screenId;
    private UUID fieldId;
    private Integer position;
    private Boolean isRequired;
    private Boolean isEditable;
    private Boolean isVisible;
}