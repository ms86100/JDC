package com.avionics_systems.admin.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssetIssueLinkResponse {

    private UUID id;
    private UUID assetId;
    private UUID issueId;
    private String linkType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
