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
public class CreateCustomFieldRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Field name is required")
    @Size(max = 255, message = "Field name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Field key is required")
    @Size(max = 255, message = "Field key must not exceed 255 characters")
    private String fieldKey;

    @NotNull(message = "Field type is required")
    private String fieldType;

    private String description;

    private String options;

    private String defaultValue;

    private String validationRules;
}