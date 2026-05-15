package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for Comment operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CommentResponse {

    @EqualsAndHashCode.Include
    private String id;

    private String issueId;
    private String authorId;
    private String authorEmail;
    private String authorDisplayName;
    private String body;
    private String bodyFormat;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime editedDate;
    private boolean edited;
    private String parentCommentId;
    private int childCommentCount;
    private String visibility;
    private String roleVisibility;
    private String groupVisibility;
    private boolean success;
    private String errorMessage;
}