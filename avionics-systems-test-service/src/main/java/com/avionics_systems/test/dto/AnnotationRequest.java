package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnotationRequest {
    @NotBlank(message = "Annotation content cannot be blank")
    @Size(max = 5000, message = "Annotation cannot exceed 5000 characters")
    private String content;

    private String type; // NOTE, BUG_INFO, ROOT_CAUSE, FIX_DESCRIPTION, GENERAL

    private UUID authorId;

    private String authorName;
}
