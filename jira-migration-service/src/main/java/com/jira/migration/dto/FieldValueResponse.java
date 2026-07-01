package com.jira.migration.dto;

import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.entity.field.IssueFieldValue;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValueResponse {
    private UUID id;
    private UUID issueId;
    private String fieldKey;
    private String fieldDisplayName;
    private Object value;
    private String formattedValue;
    private String validationStatus;
    private String validationMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FieldValueResponse fromEntity(IssueFieldValue entity, FieldDefinition fieldDef) {
        return FieldValueResponse.builder()
                .id(entity.getId())
                .issueId(entity.getIssueId())
                .fieldKey(fieldDef != null ? fieldDef.getFieldKey() : null)
                .fieldDisplayName(fieldDef != null ? fieldDef.getDisplayName() : null)
                .value(extractValue(entity, fieldDef))
                .formattedValue(entity.getFormattedValue())
                .validationStatus(entity.getValidationStatus())
                .validationMessage(entity.getValidationMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static Object extractValue(IssueFieldValue entity, FieldDefinition fieldDef) {
        if (fieldDef == null) return entity.getStringValue();

        return switch (fieldDef.getFieldType()) {
            case TEXT, TEXTAREA, RICHTEXT, SINGLE_SELECT, ISSUE_TYPE, STATUS, PRIORITY,
                    RESOLUTION, COMPONENT, VERSION, SECURITY_LEVEL, EPIC, SPRINT, PROJECT, URL, EMAIL ->
                    entity.getStringValue();
            case NUMBER, STORY_POINTS, DURATION -> entity.getLongValue();
            case DATE -> entity.getDateValue();
            case DATETIME -> entity.getDatetimeValue();
            case CHECKBOX -> entity.getBooleanValue();
            case MULTI_SELECT, LABEL, USER, GROUP -> entity.getArrayValue();
            case CUSTOM, UNKNOWN -> entity.getObjectValue() != null ?
                    entity.getObjectValue() : entity.getStringValue();
            default -> entity.getStringValue();
        };
    }
}