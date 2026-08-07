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
public class ExecutionStatusConfigRequest {

    private UUID projectId;

    @NotBlank
    private String name;

    @NotBlank
    private String displayName;

    private String color;
    private String icon;
    private Boolean isPass;
    private Boolean isFail;
    private Integer sortOrder;
    private Boolean isActive;
}
