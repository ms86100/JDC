package com.jira.portal.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPortalResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private String portalKey;
    private String baseUrl;
    private String status;
    private Boolean isPublic;
    private Boolean requireAuthentication;
    private Boolean allowAnonymousSubmissions;
    private String brandingConfig;
    private String layoutConfig;
    private String headerContent;
    private String footerContent;
    private String homepageContent;
    private UUID[] requestTypeIds;
    private String customCss;
    private String googleAnalyticsKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private UUID publishedBy;
}