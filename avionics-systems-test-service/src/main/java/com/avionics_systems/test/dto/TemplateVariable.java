package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVariable {

    private String name;
    private String type; // STRING, NUMBER, BOOLEAN, SELECT
    private String defaultValue;
    private String description;
    private Boolean required;
    private List<String> options; // For SELECT type
    private String placeholder;
}