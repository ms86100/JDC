package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepDto {

    private Integer order;

    private String stepType; // GIVEN, WHEN, THEN, AND, BUT

    private String description;

    private String expectedResult;

    private Map<String, String> parameters; // Parameter key-value pairs

    private List<String> attachments; // Evidence attachments
}