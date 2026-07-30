package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateScreenRequest {

    @NotBlank(message = "Screen name is required")
    @Size(max = 255, message = "Screen name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Screen type is required")
    private String screenType;

    private Integer position;
}