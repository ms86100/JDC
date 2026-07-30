package com.avionics_systems.admin.dto.asset;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssetIssueLinkRequest {

    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    @NotNull(message = "Issue ID is required")
    private UUID issueId;

    private String linkType;
}
