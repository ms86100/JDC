package com.avionics_systems.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic request DTO for creating/updating master data items.
 * Each master data type uses the fields relevant to it.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MasterDataRequest {

    @NotBlank(message = "Key is required")
    private String key;

    @NotBlank(message = "Display name is required")
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

    // Link type specific
    private String outwardName;
    private String inwardName;

    // Quick filter specific
    private String jqlQuery;

    // Board column template specific
    private String boardTypeId;
    private String statusCategory;
    private Integer wipLimit;
    private String statusMappings;

    // i18n specific
    private String locale;
    private String messageValue;
}
