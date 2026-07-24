package com.jira.admin.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateAssetRequest {

    @NotNull(message = "Asset type ID is required")
    private UUID assetTypeId;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    private String description;

    private String status;

    private String subStatus;

    private String location;

    private String attributes;

    private String serialNumber;
}
