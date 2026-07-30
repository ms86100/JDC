package com.avionics_systems.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AppearanceDto {
    private String id;
    private String logoUrl;
    private String faviconUrl;
    private String defaultLanguage;
    private String defaultTimezone;
    private String theme;
    private String primaryColor;
    private String secondaryColor;
    private String font;
    private Boolean enableGravatar;
    private String userDefaultAvatar;
    private LocalDateTime updatedAt;
}