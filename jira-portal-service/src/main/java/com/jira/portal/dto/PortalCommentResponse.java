package com.jira.portal.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalCommentResponse {

    private UUID id;
    private UUID requestId;
    private String content;
    private UUID authorId;
    private String authorName;
    private String authorEmail;
    private String authorType;
    private Boolean isPublic;
    private Boolean isInternal;
    private LocalDateTime createdAt;
    private String attachments;
}