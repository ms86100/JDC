package com.avionics_systems.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LicenseDto {
    private String id;
    private String licenseKey;
    private String product;
    private String tier;
    private Integer maxUsers;
    private Integer currentUsers;
    private LocalDateTime purchaseDate;
    private LocalDateTime expiryDate;
    private Boolean isEvaluation;
    private Boolean isActive;
    private String organization;
}