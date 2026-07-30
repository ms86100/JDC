package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepTagRequest {

    private List<String> tags;

    private List<String> categories;

    private List<String> labels; // Additional categorization
}