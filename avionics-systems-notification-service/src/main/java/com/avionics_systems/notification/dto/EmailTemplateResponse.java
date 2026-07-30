package com.avionics_systems.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateResponse {

    private UUID id;
    private String templateKey;
    private String name;
    private String description;
    private String subjectTemplate;
    private String bodyTemplate;
    private String eventType;
    private Boolean isDefault;
    private Boolean enabled;
    private String templateType;
    private UUID createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}