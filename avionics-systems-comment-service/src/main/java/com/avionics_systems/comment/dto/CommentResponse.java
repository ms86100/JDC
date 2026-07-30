package com.avionics_systems.comment.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID id;
    private UUID issueId;
    private UUID userId;
    private UUID parentCommentId;
    private String content;
    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean internal;

    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
}