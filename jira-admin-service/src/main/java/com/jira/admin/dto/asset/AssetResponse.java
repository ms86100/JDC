package com.jira.admin.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssetResponse {

    private UUID id;
    private UUID assetTypeId;
    private String name;
    private String description;
    private String status;
    private String subStatus;
    private String location;
    private String attributes;
    private String serialNumber;
    private String qrCodeData;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
