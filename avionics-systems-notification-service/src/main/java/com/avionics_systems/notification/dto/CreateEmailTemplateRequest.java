package com.avionics_systems.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmailTemplateRequest {

    @NotBlank(message = "{validation.email.template.key.required}")
    private String templateKey;

    @NotBlank(message = "{validation.email.template.name.required}")
    private String name;

    private String description;

    @NotBlank(message = "{validation.email.template.subject.required}")
    private String subjectTemplate;

    @NotBlank(message = "{validation.email.template.body.required}")
    private String bodyTemplate;

    @NotBlank(message = "{validation.email.template.eventType.required}")
    private String eventType;

    @Builder.Default
    private Boolean isDefault = false;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private String templateType = "THYMELEAF";

    private UUID createdBy;
}