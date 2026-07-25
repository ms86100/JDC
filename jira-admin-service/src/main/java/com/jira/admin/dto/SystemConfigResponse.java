package com.jira.admin.dto;

import com.jira.admin.entity.SystemConfigurationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SystemConfigResponse {

    private UUID id;
    private String configKey;
    private String configValue;
    private String valueType;
    private String category;
    private String description;
    private Boolean isEditable;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static SystemConfigResponse fromEntity(SystemConfigurationEntity e) {
        return SystemConfigResponse.builder()
                .id(e.getId())
                .configKey(e.getConfigKey())
                .configValue(e.getConfigValue())
                .valueType(e.getValueType())
                .category(e.getCategory())
                .description(e.getDescription())
                .isEditable(e.getIsEditable())
                .updatedAt(e.getUpdatedAt())
                .updatedBy(e.getUpdatedBy())
                .build();
    }
}
