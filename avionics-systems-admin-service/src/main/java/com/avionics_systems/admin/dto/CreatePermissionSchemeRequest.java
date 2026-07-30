package com.avionics_systems.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreatePermissionSchemeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String scope;

    private Boolean isDefault;
}