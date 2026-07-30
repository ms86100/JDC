package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO representing a Comment in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CommentDto {

    @EqualsAndHashCode.Include
    private String id;

    private String issueId;
    private String authorId;
    private String authorEmail;
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
}