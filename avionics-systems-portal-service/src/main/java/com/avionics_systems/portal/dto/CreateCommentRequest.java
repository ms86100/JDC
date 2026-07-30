package com.avionics_systems.portal.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    private String content;
    private UUID authorId;
    private String authorName;
    private String authorEmail;
    private String authorType = "CUSTOMER";
    private Boolean isPublic = true;
    private Boolean isInternal = false;
    private String attachments;
}