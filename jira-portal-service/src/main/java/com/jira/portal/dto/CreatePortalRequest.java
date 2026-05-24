package com.jira.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePortalRequest {

    @NotBlank(message = "Portal name is required")
    private String name;

    private String description;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Portal key is required")
    private String portalKey;

    private String baseUrl;
    private Boolean isPublic = false;
    private Boolean requireAuthentication = true;
    private Boolean allowAnonymousSubmissions = false;
    private String brandingConfig;
    private String layoutConfig;
    private String headerContent;
    private String footerContent;
    private String homepageContent;
    private UUID[] requestTypeIds;
    private String customCss;
    private String googleAnalyticsKey;
}