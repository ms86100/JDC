package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request DTO for updating a Comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {

    private String body;
    private String bodyFormat;
    private String visibility;
    private String roleVisibility;
    private String groupVisibility;
    private Map<String, Object> properties;
}