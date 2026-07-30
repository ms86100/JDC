package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenFieldPreview {

    private UUID fieldId;
    private String fieldName;
    private String fieldType;
    private Integer position;
    private Boolean isRequired;
    private Boolean isEditable;
    private Boolean isVisible;
}