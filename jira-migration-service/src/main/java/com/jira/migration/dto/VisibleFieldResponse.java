package com.jira.migration.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisibleFieldResponse {
    private String fieldKey;
    private String displayName;
    private String fieldType;
    private String renderer;
    private Object value;
    private boolean required;
    private boolean readOnly;
    private boolean custom;
    private int displayOrder;
}
