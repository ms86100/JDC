package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenSchemeUsageReport {

    private UUID schemeId;
    private String schemeName;
    private UUID projectId;
    private Boolean isDefault;
    private int screenCount;
}