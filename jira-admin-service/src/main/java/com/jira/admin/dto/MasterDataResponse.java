package com.jira.admin.dto;

import com.jira.admin.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generic response DTO for master data items.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MasterDataResponse {

    private UUID id;
    private String key;
    private String displayName;
    private String description;
    private String category;
    private String color;
    private String icon;
    private String iconUrl;
    private Integer sortOrder;
    private Boolean isSystem;
    private Boolean isActive;
    private Boolean isDefault;
    private Boolean isSubtask;
    private String outwardName;
    private String inwardName;
    private String jqlQuery;
    private String statusCategory;
    private Integer wipLimit;
    private String statusMappings;
    private String boardTypeId;
    private String locale;
    private String messageValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MasterDataResponse fromStatus(MasterStatusEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getStatusKey()).displayName(e.getDisplayName())
                .description(e.getDescription()).category(e.getCategory())
                .color(e.getColor()).icon(e.getIcon()).sortOrder(e.getSortOrder())
                .isSystem(e.getIsSystem()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static MasterDataResponse fromPriority(MasterPriorityEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getPriorityKey()).displayName(e.getDisplayName())
                .description(e.getDescription()).color(e.getColor())
                .iconUrl(e.getIconUrl()).sortOrder(e.getSortOrder())
                .isDefault(e.getIsDefault()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static MasterDataResponse fromIssueType(MasterIssueTypeEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getTypeKey()).displayName(e.getDisplayName())
                .description(e.getDescription()).icon(e.getIcon()).color(e.getColor())
                .isSubtask(e.getIsSubtask()).isSystem(e.getIsSystem())
                .isActive(e.getIsActive()).sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static MasterDataResponse fromResolution(MasterResolutionEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getResolutionKey()).displayName(e.getDisplayName())
                .description(e.getDescription()).sortOrder(e.getSortOrder())
                .isDefault(e.getIsDefault()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static MasterDataResponse fromLinkType(MasterLinkTypeEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getLinkKey())
                .displayName(e.getOutwardName())
                .outwardName(e.getOutwardName()).inwardName(e.getInwardName())
                .description(e.getDescription())
                .isSystem(e.getIsSystem()).isActive(e.getIsActive())
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static MasterDataResponse fromRole(MasterRoleEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getRoleKey()).displayName(e.getDisplayName())
                .description(e.getDescription())
                .isSystem(e.getIsSystem()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static MasterDataResponse fromPermission(MasterPermissionEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getPermissionKey()).displayName(e.getDisplayName())
                .description(e.getDescription()).category(e.getCategory())
                .isSystem(e.getIsSystem()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public static MasterDataResponse fromBoardType(MasterBoardTypeEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getTypeKey()).displayName(e.getDisplayName())
                .description(e.getDescription()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public static MasterDataResponse fromNotificationEvent(MasterNotificationEventEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getEventKey()).displayName(e.getDisplayName())
                .description(e.getDescription()).category(e.getCategory())
                .isSystem(e.getIsSystem()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public static MasterDataResponse fromQuickFilter(MasterQuickFilterPresetEntity e) {
        return MasterDataResponse.builder()
                .id(e.getId()).key(e.getFilterName()).displayName(e.getFilterName())
                .jqlQuery(e.getJqlQuery()).icon(e.getIcon())
                .sortOrder(e.getSortOrder())
                .isSystem(e.getIsSystem()).isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
