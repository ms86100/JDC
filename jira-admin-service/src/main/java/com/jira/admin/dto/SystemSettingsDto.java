package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SystemSettingsDto {
    private String id;
    private String settingKey;
    private String settingValue;
    private String settingType;
    private String category;
    private Boolean isSecure;
    private LocalDateTime updatedAt;
}