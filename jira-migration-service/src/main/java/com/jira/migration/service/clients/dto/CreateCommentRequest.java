package com.jira.migration.service.clients.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request DTO for creating a new Comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Comment body is required")
    private String body;

    private String bodyFormat;
    private String authorId;
    private LocalDateTime created;
    private String parentCommentId;
    private String visibility;
    private String roleVisibility;
    private String groupVisibility;
    private Map<String, Object> properties;
}