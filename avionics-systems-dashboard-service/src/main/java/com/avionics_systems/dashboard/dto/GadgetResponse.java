package com.avionics_systems.dashboard.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GadgetResponse {

    private UUID id;
    private String title;
    private String description;
    private String moduleKey;
    private String category;
    private String thumbnailUrl;
    private String configSchema;
    private String configDefaults;
    private Boolean isEnabled;
    private Boolean isSystem;
    private Boolean isSensitive;
    private String permissionType;
    private String apiVersion;
}
