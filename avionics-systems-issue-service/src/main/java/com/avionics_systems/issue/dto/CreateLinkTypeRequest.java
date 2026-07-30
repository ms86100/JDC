package com.avionics_systems.issue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLinkTypeRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Inward description is required")
    private String inward;

    @NotBlank(message = "Outward description is required")
    private String outward;
}