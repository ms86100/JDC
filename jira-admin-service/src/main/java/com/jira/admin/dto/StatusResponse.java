package com.jira.admin.dto;

import com.jira.admin.entity.StatusEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StatusResponse {

    private String id;
    private String name;
    private String description;
    private String statusCategory;
    private String statusColor;
    private String iconUrl;
    private Integer sequence;
    private Boolean isDefault;
    private Boolean isActive;
    private Boolean isArchived;
    private String lookupGroup;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StatusResponse fromEntity(StatusEntity entity) {
        return StatusResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .statusCategory(entity.getStatusCategory())
                .statusColor(entity.getStatusColor())
                .iconUrl(entity.getIconUrl())
                .sequence(entity.getSequence())
                .isDefault(entity.getIsDefault())
                .isActive(entity.getIsActive())
                .isArchived(entity.getIsArchived())
                .lookupGroup(entity.getLookupGroup())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
