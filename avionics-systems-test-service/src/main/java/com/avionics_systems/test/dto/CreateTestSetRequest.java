package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestSetRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String name;

    private String description;

    private String testType;

    private List<String> labels;
}