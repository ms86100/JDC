package com.avionics_systems.admin.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateAssetTypeRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    private String description;

    private String attributeSchema;

    private String permissionScheme;
}
