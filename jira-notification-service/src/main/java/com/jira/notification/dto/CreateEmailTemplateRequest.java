package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmailTemplateRequest {

    @NotBlank(message = "Template key is required")
    private String templateKey;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Subject template is required")
    private String subjectTemplate;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    @NotBlank(message = "Event type is required")
    private String eventType;

    @Builder.Default
    private Boolean isDefault = false;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private String templateType = "THYMELEAF";

    private UUID createdBy;
}