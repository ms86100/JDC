package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestStatusConfigRequest {

    private UUID projectId;

    @NotBlank
    private String name;

    @NotBlank
    private String displayName;

    private String color;
    private String icon;
    private String category;
    private Boolean isDefault;
    private Boolean isFinal;
    private Integer sortOrder;
    private Boolean isActive;
}
